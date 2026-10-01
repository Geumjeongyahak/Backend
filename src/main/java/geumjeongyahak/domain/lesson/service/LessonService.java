package geumjeongyahak.domain.lesson.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.event.LessonDailyScheduleSyncRequestedEvent;
import geumjeongyahak.domain.lesson.exception.InvalidLessonScheduleException;
import geumjeongyahak.domain.lesson.exception.InvalidLessonStatusTransitionException;
import geumjeongyahak.domain.lesson.exception.LessonDuplicateException;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.schedule.LessonGenerator;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.ConflictExclusion;
import geumjeongyahak.domain.lesson.v1.dto.request.CreateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;
import geumjeongyahak.domain.auth.enums.RoleType;
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
    private final EventPublisher eventPublisher;
    private final DailyScheduleProxyService dailyScheduleProxyService;
    private final TeacherLessonConflictChecker conflictChecker;
    private final LessonGenerator lessonGenerator;

    @Transactional
    public LessonDetailResponse createLesson(
        Long requesterId,
        CreateLessonRequest request
    ) {
        log.debug("수업 생성 요청 (requesterId={})", requesterId);

        Subject subject = subjectProxyService.getById(request.subjectId());

        User teacher = userProxyService.getById(request.teacherId());
        validateTeacherAssignable(teacher);
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
        publishDailyScheduleSyncFor(List.of(saved));
        log.debug("수업 생성 완료 (lessonId={})", saved.getId());

        return LessonDetailResponse.from(saved);
    }

    public List<LessonSummaryResponse> getAllLessons(LessonRangeRequest request) {
        log.debug("전체 수업 목록 조회 요청");
        List<Lesson> lessonList = lessonRepository
            .findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(request.from(), request.to());
        Map<DailyScheduleKey, DailySchedule> dailySchedules = getDailyScheduleMap(request.from(), request.to());
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
        Map<DailyScheduleKey, DailySchedule> dailySchedules = getDailyScheduleMap(request.from(), request.to());
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
            validateTeacherAssignable(teacher);
        }
        userProxyService.fillDefaultClassroomIfMissing(teacher, subject.getClassroom());

        // 변경 반영
        lesson.update(subject, teacher, newDate, newStart, newEnd, newPeriod);
        publishDailyScheduleSync(List.of(
            DailyScheduleKey.from(lesson),
            new DailyScheduleKey(previousClassroomId, previousDate)
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

    // ── 이벤트 핸들러 전용 내부 메서드 ─────────────────────────────────────────

    /**
     * 과목 생성 이벤트 처리용 - startAt~endAt 사이 dayOfWeek에 해당하는 날짜에 수업을 자동 생성한다.
     * 특정 날짜에 교사 시간 충돌이 있으면 해당 날짜만 스킵하고 계속 진행한다 ({@link LessonGenerator}).
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

        Subject subject = subjectProxyService.getById(subjectId);
        User teacher = userProxyService.getById(teacherId);
        validateTeacherAssignable(teacher);

        List<Lesson> created = lessonGenerator.generate(
            subject, teacher, startAt, endAt, dayOfWeek, startTime, endTime, period
        );
        publishDailyScheduleSyncFor(created);

        log.debug("수업 자동 생성 완료 (subjectId={}, 생성={}건)", subjectId, created.size());
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
        User newTeacher = userProxyService.getById(teacherId);
        validateTeacherAssignable(newTeacher);

        List<Lesson> lessons = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            );

        lessons.forEach(lesson -> lesson.changeTeacher(newTeacher));
        publishDailyScheduleSyncFor(lessons);
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

        lessons.forEach(Lesson::softDelete);
        publishDailyScheduleSyncFor(lessons);
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

        lessons.forEach(lesson -> lesson.changeSchedule(startTime, endTime, period));
        publishDailyScheduleSyncFor(lessons);
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
            lesson.softDelete();
            publishDailyScheduleSyncFor(List.of(lesson));
        }
        log.debug("수업 삭제 완료 (lessonId={})", lessonId);
    }

    private void publishDailyScheduleSyncFor(Collection<Lesson> lessons) {
        publishDailyScheduleSync(lessons.stream().map(DailyScheduleKey::from).toList());
    }

    /** 바뀐 (분반, 날짜)마다 DailySchedule 동기화를 한 번만 요청한다. */
    private void publishDailyScheduleSync(Collection<DailyScheduleKey> keys) {
        new LinkedHashSet<>(keys).forEach(key -> eventPublisher.publish(
            new LessonDailyScheduleSyncRequestedEvent(key.classroomId(), key.lessonDate())
        ));
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

    private Map<DailyScheduleKey, DailySchedule> getDailyScheduleMap(LocalDate from, LocalDate to) {
        return dailyScheduleProxyService.findAllActiveBetween(from, to).stream()
            .collect(Collectors.toMap(
                DailyScheduleKey::from,
                Function.identity()
            ));
    }

    private LessonSummaryResponse toSummaryResponse(
        Lesson lesson,
        Map<DailyScheduleKey, DailySchedule> dailySchedules,
        Map<Long, DailyTeacherAttendance> teacherAttendances
    ) {
        DailySchedule dailySchedule = dailySchedules.get(DailyScheduleKey.from(lesson));
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
        Map<DailyScheduleKey, DailySchedule> dailySchedules
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

    private record DailyScheduleKey(Long classroomId, LocalDate lessonDate) {

        private static DailyScheduleKey from(DailySchedule dailySchedule) {
            return new DailyScheduleKey(
                dailySchedule.getClassroom().getId(),
                dailySchedule.getLessonDate()
            );
        }

        private static DailyScheduleKey from(Lesson lesson) {
            return new DailyScheduleKey(
                lesson.getSubject().getClassroom().getId(),
                lesson.getDate()
            );
        }
    }
}
