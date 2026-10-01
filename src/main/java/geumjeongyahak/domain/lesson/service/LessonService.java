package geumjeongyahak.domain.lesson.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.exception.InvalidLessonScheduleException;
import geumjeongyahak.domain.lesson.exception.InvalidLessonStatusTransitionException;
import geumjeongyahak.domain.lesson.exception.LessonDuplicateException;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.schedule.DailyScheduleSyncPublisher;
import geumjeongyahak.domain.lesson.service.schedule.DailyScheduleSyncPublisher.ClassroomDate;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.ConflictExclusion;
import geumjeongyahak.domain.lesson.v1.dto.request.CreateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SubjectProxyService subjectProxyService;
    private final UserProxyService userProxyService;
    private final DailyScheduleProxyService dailyScheduleProxyService;
    private final TeacherLessonConflictChecker conflictChecker;
    private final DailyScheduleSyncPublisher syncPublisher;

    @Transactional
    public LessonDetailResponse createLesson(
        Long requesterId,
        CreateLessonRequest request
    ) {
        log.debug("수업 생성 요청 (requesterId={})", requesterId);

        Subject subject = subjectProxyService.getById(request.subjectId());

        User teacher = userProxyService.getById(request.teacherId());
        teacher.validateCanTeach();
        userProxyService.fillDefaultClassroomIfMissing(teacher, subject.getClassroom());

        if (conflictChecker.hasConflict(
            teacher.getId(),
            List.of(request.date()),
            request.startTime(),
            request.endTime(),
            ConflictExclusion.NONE
        )) {
            log.info("수업 생성 실패 - 시간대가 겹치는 수업이 존재합니다.");
            throw new LessonDuplicateException("시간대가 겹치는 수업이 존재합니다.");
        }

        Lesson lesson = new Lesson(
            subject,
            teacher,
            request.date(),
            request.startTime(),
            request.endTime(),
            request.period()
        );

        Lesson saved = lessonRepository.save(lesson);
        syncPublisher.publishFor(List.of(saved));
        log.debug("수업 생성 완료 (lessonId={})", saved.getId());

        return LessonDetailResponse.from(saved);
    }

    public List<LessonSummaryResponse> getAllLessons(LessonRangeRequest request) {
        log.debug("전체 수업 목록 조회 요청");
        List<Lesson> lessonList = lessonRepository
            .findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(request.from(), request.to());
        Map<ClassroomDate, DailySchedule> dailySchedules = getDailyScheduleMap(request.from(), request.to());
        Map<Long, DailyTeacherAttendance> teacherAttendances = getTeacherAttendanceMap(dailySchedules);
        log.debug("전체 수업 목록 조회 완료 - 총 {}개", lessonList.size());
        return lessonList.stream()
            .map(lesson -> toSummaryResponse(lesson, dailySchedules, teacherAttendances))
            .toList();
    }

    public List<LessonSummaryResponse> getMyLessons(
        Long userId,
        LessonRangeRequest request
    ) {
        log.debug("내 수업 목록 조회 요청");
        List<Lesson> lessonList = lessonRepository
            .findAllByTeacherIdAndIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
                userId, request.from(), request.to()
            );
        Map<ClassroomDate, DailySchedule> dailySchedules = getDailyScheduleMap(request.from(), request.to());
        Map<Long, DailyTeacherAttendance> teacherAttendances = getTeacherAttendanceMap(dailySchedules);
        log.debug("내 수업 목록 조회 완료 - 총 {}개", lessonList.size());
        return lessonList.stream()
            .map(lesson -> toSummaryResponse(lesson, dailySchedules, teacherAttendances))
            .toList();
    }

    public LessonDetailResponse getLessonDetail(Long teacherId, Long lessonId, boolean canAccessAnyLesson) {
        log.debug("수업 상세 조회 요청");
        Optional<Lesson> lessonOpt = canAccessAnyLesson
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId);

        return lessonOpt
            .map(this::toDetailResponse)
            .orElseThrow(() -> {
                log.warn("수업 상세 조회 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
                return new LessonNotFoundException(lessonId);
            });
    }

    @Transactional
    public LessonDetailResponse updateLesson(Long lessonId, UpdateLessonRequest request) {
        log.debug("수업 수정 요청 (lessonId={})", lessonId);
        Lesson lesson = lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            .orElseThrow(() -> {
                log.info("수업 수정 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
                return new LessonNotFoundException(lessonId);
            });
        Long previousClassroomId = lesson.getSubject().getClassroom().getId();
        LocalDate previousDate = lesson.getDate();

        // 최종 값
        Long newSubjectId = request.subjectId() != null ? request.subjectId() : lesson.getSubject().getId();
        Long newTeacherId = request.teacherId() != null ? request.teacherId() : lesson.getTeacher().getId();
        LocalDate newDate = request.date() != null ? request.date() : lesson.getDate();
        LocalTime newStart = request.startTime() != null ? request.startTime() : lesson.getStartTime();
        LocalTime newEnd = request.endTime() != null ? request.endTime() : lesson.getEndTime();
        Integer newPeriod = request.period() != null ? request.period() : lesson.getPeriod();

        // 시간 유효성 검증
        if (!newStart.isBefore(newEnd)) {
            log.info("수업 수정 실패 - 시작 시간은 종료 시간보다 빨라야 합니다.");
            throw new InvalidLessonScheduleException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }

        // 중복 검사 (merge 기준, 자기 자신 제외)
        boolean overlap = conflictChecker.hasConflict(
            newTeacherId,
            List.of(newDate),
            newStart,
            newEnd,
            ConflictExclusion.ofLesson(lesson.getId())
        );

        if (overlap) {
            log.info("수업 수정 실패 - 시간대가 겹치는 수업이 존재합니다.");
            throw new LessonDuplicateException("시간대가 겹치는 수업이 존재합니다.");
        }

        // 연관 엔티티가 바뀌는 경우만 조회
        Subject subject = lesson.getSubject();
        if (!subject.getId().equals(newSubjectId)) {
            subject = subjectProxyService.getById(newSubjectId);
        }

        User teacher = lesson.getTeacher();
        if (!teacher.getId().equals(newTeacherId)) {
            teacher = userProxyService.getById(newTeacherId);
            teacher.validateCanTeach();
        }
        userProxyService.fillDefaultClassroomIfMissing(teacher, subject.getClassroom());

        // 변경 반영
        lesson.update(subject, teacher, newDate, newStart, newEnd, newPeriod);
        syncPublisher.publish(List.of(
            ClassroomDate.of(lesson),
            new ClassroomDate(previousClassroomId, previousDate)
        ));
        log.debug("수업 수정 완료 (lessonId={})", lessonId);
        return LessonDetailResponse.from(lesson);
    }

    @Transactional
    public LessonDetailResponse updateLessonStatus(
        Long teacherId,
        Long lessonId,
        LessonStatus status,
        boolean canAccessAnyLesson
    ) {
        log.debug("수업 상태 변경 요청 (lessonId={})", lessonId);
        Lesson lesson = (canAccessAnyLesson
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId)
        ).orElseThrow(() -> {
            log.warn("수업 상태 변경 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
            return new LessonNotFoundException(lessonId);
        });
        validateLessonStatusTransition(lesson.getStatus(), status);
        lesson.updateStatus(status);
        log.debug("수업 상태 변경 완료 (status={})", status);
        return LessonDetailResponse.from(lesson);
    }

    private void validateLessonStatusTransition(LessonStatus currentStatus, LessonStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }

        if (currentStatus == LessonStatus.SCHEDULED
            && (nextStatus == LessonStatus.COMPLETED || nextStatus == LessonStatus.CANCELED)) {
            return;
        }

        throw new InvalidLessonStatusTransitionException(currentStatus, nextStatus);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        log.debug("수업 삭제 요청 (lessonId={})", lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> {
                log.info("수업 삭제 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
                return new LessonNotFoundException(lessonId);
            });
        if (!lesson.getIsDeleted()) {
            lesson.softDelete();
            syncPublisher.publishFor(List.of(lesson));
        }
        log.debug("수업 삭제 완료 (lessonId={})", lessonId);
    }

    private LessonDetailResponse toDetailResponse(Lesson lesson) {
        DailySchedule dailySchedule = dailyScheduleProxyService.findActiveByClassroomIdAndLessonDate(
            lesson.getSubject().getClassroom().getId(),
            lesson.getDate()
        );
        if (dailySchedule == null) {
            return LessonDetailResponse.from(lesson);
        }
        DailyTeacherAttendance teacherAttendance = dailyScheduleProxyService
            .findActiveTeacherAttendanceByDailyScheduleId(dailySchedule.getId())
            .orElse(null);
        return LessonDetailResponse.from(
            lesson,
            dailySchedule.getId(),
            dailySchedule.isExchanged(),
            dailySchedule.isAbsent(),
            dailySchedule.getExchangedLessonDate(),
            teacherAttendance
        );
    }

    private Map<ClassroomDate, DailySchedule> getDailyScheduleMap(LocalDate from, LocalDate to) {
        return dailyScheduleProxyService.findAllActiveBetween(from, to).stream()
            .collect(Collectors.toMap(
                this::classroomDateOf,
                Function.identity()
            ));
    }

    private LessonSummaryResponse toSummaryResponse(
        Lesson lesson,
        Map<ClassroomDate, DailySchedule> dailySchedules,
        Map<Long, DailyTeacherAttendance> teacherAttendances
    ) {
        DailySchedule dailySchedule = dailySchedules.get(ClassroomDate.of(lesson));
        if (dailySchedule == null) {
            return LessonSummaryResponse.from(lesson);
        }
        return LessonSummaryResponse.from(
            lesson,
            dailySchedule.isExchanged(),
            dailySchedule.isAbsent(),
            dailySchedule.getExchangedLessonDate(),
            teacherAttendances.get(dailySchedule.getId())
        );
    }

    private Map<Long, DailyTeacherAttendance> getTeacherAttendanceMap(
        Map<ClassroomDate, DailySchedule> dailySchedules
    ) {
        return dailyScheduleProxyService.findActiveTeacherAttendancesByDailyScheduleIds(
                dailySchedules.values().stream()
                    .map(DailySchedule::getId)
                    .collect(Collectors.toSet())
            )
            .stream()
            .collect(Collectors.toMap(
                attendance -> attendance.getDailySchedule().getId(),
                Function.identity()
            ));
    }

    private ClassroomDate classroomDateOf(DailySchedule dailySchedule) {
        return new ClassroomDate(dailySchedule.getClassroom().getId(), dailySchedule.getLessonDate());
    }
}
