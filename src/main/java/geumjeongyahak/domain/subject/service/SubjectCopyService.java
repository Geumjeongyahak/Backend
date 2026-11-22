package geumjeongyahak.domain.subject.service;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.event.SubjectTeacherAssignedEvent;
import geumjeongyahak.domain.subject.event.SubjectsCopiedEvent;
import geumjeongyahak.domain.subject.event.SubjectsCopiedEvent.CopiedSubject;
import geumjeongyahak.domain.subject.exception.SubjectCopyConflictException;
import geumjeongyahak.domain.subject.exception.SubjectNotCopyableException;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.subject.v1.dto.request.SubjectCopyRequest;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectCopyFailure;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectCopyResponse;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보낸 과목들을 새 기간으로 한 번에 복사한다 (#242). 판정은 과목 생성과 같은 {@link SubjectScheduleValidator}를 쓴다.
 * 하나라도 실패하면 실패 목록을 모두 모아 409로 끝내고 아무것도 저장하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectCopyService {

    private static final String COLLIDES_WITH_REQUESTED = "같이 보낸 과목과 같은 분반·요일·시간이 겹칩니다.";
    private static final String TEACHER_SPLIT_IN_REQUEST = "같이 보낸 과목에서 같은 담당 교사가 다른 분반·요일을 맡습니다.";

    private final SubjectRepository subjectRepository;
    private final SubjectScheduleValidator validator;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public SubjectCopyResponse copy(SubjectCopyRequest request) {
        List<Subject> sources = loadSources(request.subjectIds());
        LocalDate startAt = request.startAt();
        LocalDate endAt = request.endAt();
        sources.forEach(source ->
            validator.validateCreateSchedule(startAt, endAt, source.getStartTime(), source.getEndTime())
        );

        List<SubjectCopyFailure> failures = new ArrayList<>(conflictsWithinRequest(sources, startAt, endAt));
        Set<Long> collided = failures.stream().map(SubjectCopyFailure::sourceSubjectId).collect(Collectors.toSet());
        sources.stream()
            .filter(source -> !collided.contains(source.getId()))
            .map(source -> validateCopy(source, startAt, endAt))
            .flatMap(Optional::stream)
            .forEach(failures::add);
        if (!failures.isEmpty()) {
            throw new SubjectCopyConflictException(failures);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Subject> copies = subjectRepository.saveAll(
            sources.stream().map(source -> copyOf(source, startAt, endAt, now)).toList()
        );
        publishEvents(copies, now.toLocalDate());
        log.info("시간표 기간 복사 완료 (과목={}건, 기간={}~{})", copies.size(), startAt, endAt);
        return SubjectCopyResponse.of(copies.stream().map(SubjectDetailResponse::from).toList());
    }

    private List<Subject> loadSources(List<Long> subjectIds) {
        Set<Long> unique = new HashSet<>(subjectIds);
        if (unique.size() != subjectIds.size()) {
            throw new SubjectNotCopyableException("복사할 과목 ID가 중복되었습니다.");
        }
        Map<Long, Subject> found = subjectRepository.findAllByIdIn(unique).stream()
            .filter(subject -> Boolean.TRUE.equals(subject.getIsActive()))
            .collect(Collectors.toMap(Subject::getId, Function.identity()));
        List<Long> missing = subjectIds.stream().filter(id -> !found.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new SubjectNotCopyableException("없거나 삭제된 과목은 복사할 수 없습니다: " + missing);
        }
        return subjectIds.stream().map(found::get).toList();
    }

    /**
     * 보낸 과목끼리의 충돌. 복사본은 아직 저장 전이라 DB 검증으로는 서로가 안 보인다.
     * 판정은 과목 생성 검증과 같게 맞춘다 — 같은 칸은 새 기간에 그 요일이 있을 때만, 교사는 하루치 일정 하나(#199).
     */
    private List<SubjectCopyFailure> conflictsWithinRequest(List<Subject> sources, LocalDate startAt, LocalDate endAt) {
        Map<Subject, String> reasons = new LinkedHashMap<>();

        sources.stream()
            .collect(Collectors.groupingBy(source -> new Cell(source.getClassroom().getId(), source.getDayOfWeek())))
            .forEach((cell, sameCell) -> {
                if (!hasWeekday(startAt, endAt, cell.dayOfWeek())) {
                    return;
                }
                // 모든 쌍을 본다 — 이웃끼리만 보면 09–12와 11:30–13 같은 충돌 참여자를 놓친다
                for (int i = 0; i < sameCell.size(); i++) {
                    for (int j = i + 1; j < sameCell.size(); j++) {
                        if (timesOverlap(sameCell.get(i), sameCell.get(j))) {
                            reasons.putIfAbsent(sameCell.get(i), COLLIDES_WITH_REQUESTED);
                            reasons.putIfAbsent(sameCell.get(j), COLLIDES_WITH_REQUESTED);
                        }
                    }
                }
            });

        sources.stream()
            .filter(source -> source.getTeacher() != null)
            .collect(Collectors.groupingBy(source -> source.getTeacher().getId()))
            .values()
            .stream()
            .filter(sameTeacher -> sameTeacher.stream()
                .map(source -> new Cell(source.getClassroom().getId(), source.getDayOfWeek()))
                .distinct()
                .count() > 1)
            .flatMap(List::stream)
            .forEach(source -> reasons.putIfAbsent(source, TEACHER_SPLIT_IN_REQUEST));

        return reasons.entrySet().stream()
            .map(entry -> SubjectCopyFailure.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    private static boolean timesOverlap(Subject left, Subject right) {
        return left.getStartTime().isBefore(right.getEndTime()) && right.getStartTime().isBefore(left.getEndTime());
    }

    private static boolean hasWeekday(LocalDate startAt, LocalDate endAt, DayOfWeek dayOfWeek) {
        return !startAt.with(TemporalAdjusters.nextOrSame(dayOfWeek)).isAfter(endAt);
    }

    /** 과목 생성과 같은 검증. 실패하면 사유를 돌려준다. */
    private Optional<SubjectCopyFailure> validateCopy(Subject source, LocalDate startAt, LocalDate endAt) {
        try {
            validator.validateSubjectDuplicate(
                null,
                source.getClassroom().getId(),
                source.getDayOfWeek(),
                startAt,
                endAt,
                source.getStartTime(),
                source.getEndTime()
            );
            if (source.getTeacher() != null) {
                Long teacherId = source.getTeacher().getId();
                validator.validateTeacherScheduleAssignable(teacherId, copyOf(source, startAt, endAt, null));
                validator.validateNoTeacherConflictForSchedule(
                    source.getId(),
                    teacherId,
                    LocalDate.now(clock),
                    lessonStartAt(startAt),
                    endAt,
                    source.getDayOfWeek(),
                    source.getStartTime(),
                    source.getEndTime(),
                    true
                );
            }
            return Optional.empty();
        } catch (BusinessException rejected) {
            return Optional.of(SubjectCopyFailure.of(source, rejected.getMessage()));
        }
    }

    private Subject copyOf(Subject source, LocalDate startAt, LocalDate endAt, LocalDateTime now) {
        return new Subject(
            source.getClassroom(),
            source.getTeacher(),
            source.getName(),
            startAt,
            endAt,
            source.getDayOfWeek(),
            source.getStartTime(),
            source.getEndTime(),
            source.getPeriod(),
            source.getTeacher() != null ? now : null,
            source.getDescription()
        );
    }

    /**
     * 교사가 있는 복사본: 교사 배정 이벤트는 (교사, 분반)마다 한 번(기본 분반 채우기용),
     * 수업 생성은 한 이벤트로 묶어 (분반, 날짜) 동기화를 한 번씩만 한다.
     */
    private void publishEvents(List<Subject> copies, LocalDate today) {
        List<Subject> withTeacher = copies.stream().filter(copy -> copy.getTeacher() != null).toList();
        withTeacher.stream()
            .collect(Collectors.toMap(
                copy -> List.of(copy.getTeacher().getId(), copy.getClassroom().getId()),
                Function.identity(),
                (first, second) -> first
            ))
            .values()
            .forEach(copy -> eventPublisher.publish(new SubjectTeacherAssignedEvent(
                copy.getId(), copy.getClassroom().getId(), copy.getTeacher().getId(), today
            )));

        List<CopiedSubject> lessonPlans = withTeacher.stream()
            .filter(copy -> !lessonStartAt(copy.getStartAt()).isAfter(copy.getEndAt()))
            .map(copy -> new CopiedSubject(
                copy.getId(),
                copy.getTeacher().getId(),
                lessonStartAt(copy.getStartAt()),
                copy.getEndAt(),
                copy.getDayOfWeek(),
                copy.getStartTime(),
                copy.getEndTime(),
                copy.getPeriod()
            ))
            .toList();
        if (!lessonPlans.isEmpty()) {
            eventPublisher.publish(new SubjectsCopiedEvent(lessonPlans));
        }
    }

    /** 과목 생성과 같다: 지난 날짜에는 수업을 만들지 않는다. */
    private LocalDate lessonStartAt(LocalDate startAt) {
        LocalDate today = LocalDate.now(clock);
        return startAt.isAfter(today) ? startAt : today;
    }

    private record Cell(Long classroomId, DayOfWeek dayOfWeek) {
    }
}
