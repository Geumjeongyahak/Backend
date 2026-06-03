package geumjeongyahak.domain.lesson.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.event.LessonDailyScheduleSyncRequestedEvent;
import geumjeongyahak.domain.lesson.exception.InvalidLessonScheduleException;
import geumjeongyahak.domain.lesson.exception.InvalidLessonStatusTransitionException;
import geumjeongyahak.domain.lesson.exception.LessonDuplicateException;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.v1.dto.request.CreateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.SubjectNotFoundException;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final DailyScheduleProxyService dailyScheduleProxyService;


    @Transactional
    public LessonDetailResponse createLesson(
        Long requesterId,
        CreateLessonRequest request
    ) {
        log.debug("수업 생성 요청 (requesterId={})", requesterId);

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> {
                log.info("수업 생성 실패 - 과목을 찾을 수 없습니다. ID: {}", request.subjectId());
                return new SubjectNotFoundException(request.subjectId());
            });

        User teacher = userRepository.findById(request.teacherId())
            .orElseThrow(() -> {
                log.info("수업 생성 실패 - 교사를 찾을 수 없습니다. ID: {}", request.teacherId());
                return new UserNotFoundException(request.teacherId());
            });
        validateTeacherAssignable(teacher);

        // 같은 teacher + 같은 date 기준 겹치는 시간이 있는지 확인
        if (lessonRepository.existsByTeacherIdAndDateAndIsDeletedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            teacher.getId(),
            request.date(),
            request.endTime(),
            request.startTime()
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
        publishDailyScheduleSync(saved);
        log.debug("수업 생성 완료 (lessonId={})", saved.getId());

        return LessonDetailResponse.from(saved);
    }

    public List<LessonSummaryResponse> getAllLessons(LessonRangeRequest request) {
        log.debug("전체 수업 목록 조회 요청");
        List<Lesson> lessonList = lessonRepository
            .findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(request.from(), request.to());
        log.debug("전체 수업 목록 조회 완료 - 총 {}개", lessonList.size());
        return lessonList.stream()
            .map(LessonSummaryResponse::from)
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
        log.debug("내 수업 목록 조회 완료 - 총 {}개", lessonList.size());
        return lessonList.stream()
            .map(LessonSummaryResponse::from)
            .toList();
    }

    public LessonDetailResponse getLessonDetail(Long teacherId, Long lessonId, boolean canAccessAnyLesson) {
        log.debug("수업 상세 조회 요청");
        Optional<Lesson> lessonOpt = canAccessAnyLesson
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId);

        return lessonOpt
            .map(lesson -> LessonDetailResponse.from(lesson, findDailyScheduleId(lesson)))
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
        boolean overlap = lessonRepository
            .existsByTeacherIdAndDateAndIsDeletedFalseAndIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                newTeacherId,
                newDate,
                lesson.getId(),
                newEnd,
                newStart
            );

        if (overlap) {
            log.info("수업 수정 실패 - 시간대가 겹치는 수업이 존재합니다.");
            throw new LessonDuplicateException("시간대가 겹치는 수업이 존재합니다.");
        }

        // 연관 엔티티가 바뀌는 경우만 조회
        Subject subject = lesson.getSubject();
        if (!subject.getId().equals(newSubjectId)) {
            subject = subjectRepository.findById(newSubjectId)
                .orElseThrow(() -> {
                    log.info("수업 수정 실패 - 과목을 찾을 수 없습니다. ID: {}", newSubjectId);
                    return new SubjectNotFoundException(newSubjectId);
                });
        }

        User teacher = lesson.getTeacher();
        if (!teacher.getId().equals(newTeacherId)) {
            teacher = userRepository.findById(newTeacherId)
                .orElseThrow(() -> {
                    log.info("수업 수정 실패 - 교사를 찾을 수 없습니다. ID: {}", newTeacherId);
                    return new UserNotFoundException(newTeacherId);
                });
            validateTeacherAssignable(teacher);
        }

        // 변경 반영
        lesson.update(subject, teacher, newDate, newStart, newEnd, newPeriod);
        publishDailyScheduleSync(lesson);
        if (!previousClassroomId.equals(subject.getClassroom().getId()) || !previousDate.equals(newDate)) {
            publishDailyScheduleSync(previousClassroomId, previousDate);
        }
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

    // ── 이벤트 핸들러 전용 내부 메서드 ─────────────────────────────────────────

    /**
     * 과목 생성 이벤트 처리용 - startAt~endAt 사이 dayOfWeek에 해당하는 날짜에 수업을 자동 생성한다.
     * 특정 날짜에 교사 시간 충돌이 있으면 해당 날짜만 스킵하고 계속 진행한다.
     */
    @Transactional
    public void createLessonsFromSubject(
        Long subjectId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int period
    ) {
        log.debug("과목 수업 자동 생성 (subjectId={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new SubjectNotFoundException(subjectId));
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new UserNotFoundException(teacherId));
        validateTeacherAssignable(teacher);

        List<LocalDate> dates = startAt.datesUntil(endAt.plusDays(1))
            .filter(d -> d.getDayOfWeek() == dayOfWeek)
            .toList();

        int created = 0;
        for (LocalDate date : dates) {
            boolean conflict = lessonRepository
                .existsByTeacherIdAndDateAndIsDeletedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                    teacherId, date, endTime, startTime
                );
            if (conflict) {
                log.warn("수업 자동 생성 스킵 - 교사 시간 충돌 (date={}, teacherId={})", date, teacherId);
                continue;
            }
            Lesson lesson = lessonRepository.save(new Lesson(subject, teacher, date, startTime, endTime, period));
            publishDailyScheduleSync(lesson);
            created++;
        }

        log.debug("수업 자동 생성 완료 (subjectId={}, 생성={}건, 스킵={}건)", subjectId, created, dates.size() - created);
    }

    private void validateTeacherAssignable(User teacher) {
        if (teacher.getRole() != RoleType.VOLUNTEER
            && teacher.getRole() != RoleType.MANAGER
            && teacher.getRole() != RoleType.ADMIN) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "봉사자, 매니저 또는 관리자 사용자만 교사로 배정할 수 있습니다.");
        }
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
    public void assignTeacherToSubjectScheduledLessons(
        Long subjectId,
        Long teacherId,
        LocalDate from
    ) {
        log.debug("과목 담당 교사 배정에 따른 수업 교사 변경 (subjectId={}, teacherId={})", subjectId, teacherId);
        User newTeacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new UserNotFoundException(teacherId));
        validateTeacherAssignable(newTeacher);

        List<Lesson> lessons = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            );

        lessons.forEach(lesson -> {
            lesson.changeTeacher(newTeacher);
            publishDailyScheduleSync(lesson);
        });
    }

    @Transactional
    public void deleteFutureSubjectScheduledLessons(Long subjectId, LocalDate from) {
        log.debug("과목 변경에 따른 미래 예정 수업 삭제 (subjectId={}, from={})", subjectId, from);
        List<Lesson> lessons = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            );

        lessons.forEach(lesson -> {
            Long classroomId = lesson.getSubject().getClassroom().getId();
            LocalDate date = lesson.getDate();
            lesson.softDelete();
            publishDailyScheduleSync(classroomId, date);
        });
    }

    @Transactional
    public void updateSubjectScheduledLessonsSchedule(
        Long subjectId,
        LocalDate from,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        log.debug("과목 일정 변경에 따른 수업 시간 변경 (subjectId={})", subjectId);
        List<Lesson> lessons = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            );

        lessons.forEach(lesson -> {
            lesson.changeSchedule(startTime, endTime, period);
            publishDailyScheduleSync(lesson);
        });
    }

    @Transactional
    public void recreateSubjectScheduledLessons(
        Long subjectId,
        Long teacherId,
        LocalDate effectiveFrom,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        log.debug("과목 일정 변경에 따른 수업 재생성 (subjectId={})", subjectId);
        deleteFutureSubjectScheduledLessons(subjectId, effectiveFrom);
        if (teacherId == null || startAt.isAfter(endAt)) {
            return;
        }
        createLessonsFromSubject(
            subjectId,
            teacherId,
            startAt,
            endAt,
            dayOfWeek,
            startTime,
            endTime,
            period
        );
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
            Long classroomId = lesson.getSubject().getClassroom().getId();
            LocalDate date = lesson.getDate();
            lesson.softDelete();
            publishDailyScheduleSync(classroomId, date);
        }
        log.debug("수업 삭제 완료 (lessonId={})", lessonId);
    }

    private void publishDailyScheduleSync(Lesson lesson) {
        publishDailyScheduleSync(lesson.getSubject().getClassroom().getId(), lesson.getDate());
    }

    private void publishDailyScheduleSync(Long classroomId, LocalDate lessonDate) {
        eventPublisher.publish(new LessonDailyScheduleSyncRequestedEvent(classroomId, lessonDate));
    }

    private Long findDailyScheduleId(Lesson lesson) {
        return dailyScheduleProxyService.findActiveIdByClassroomIdAndLessonDate(
            lesson.getSubject().getClassroom().getId(),
            lesson.getDate()
        );
    }
}
