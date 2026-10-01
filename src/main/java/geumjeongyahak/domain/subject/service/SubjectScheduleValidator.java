package geumjeongyahak.domain.subject.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.request.service.AbsenceRequestProxyService;
import geumjeongyahak.domain.request.service.LessonExchangeRequestProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.SubjectDuplicateException;
import geumjeongyahak.domain.subject.exception.SubjectOperationPeriodExceededException;
import geumjeongyahak.domain.subject.exception.SubjectTeacherAssignmentConflictException;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 과목 생성·일정 변경·교사 배정 전에 거는 검증. 실패하면 예외를 던진다. */
@Component
@RequiredArgsConstructor
class SubjectScheduleValidator {

    private static final long MAX_SUBJECT_OPERATION_DAYS = 365;

    private final SubjectRepository subjectRepository;
    private final LessonProxyService lessonProxyService;
    private final AbsenceRequestProxyService absenceRequestProxyService;
    private final LessonExchangeRequestProxyService lessonExchangeRequestProxyService;

    void validateSchedule(LocalDate startAt, LocalDate endAt, LocalTime startTime, LocalTime endTime) {
        if (startAt.isAfter(endAt)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "startAt은 endAt보다 늦을 수 없습니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "startTime은 endTime보다 빨라야 합니다.");
        }
    }

    void validateCreateSchedule(LocalDate startAt, LocalDate endAt, LocalTime startTime, LocalTime endTime) {
        validateSchedule(startAt, endAt, startTime, endTime);

        long operationDays = ChronoUnit.DAYS.between(startAt, endAt) + 1;
        if (operationDays > MAX_SUBJECT_OPERATION_DAYS) {
            throw new SubjectOperationPeriodExceededException(MAX_SUBJECT_OPERATION_DAYS);
        }
    }

    /** 같은 분반에 실제 수업 날짜와 시간이 겹치는 다른 과목이 있으면 거절한다. subjectId는 자기 자신 제외용(신규면 null). */
    void validateSubjectDuplicate(
        Long subjectId,
        Long classroomId,
        DayOfWeek dayOfWeek,
        LocalDate startAt,
        LocalDate endAt,
        LocalTime startTime,
        LocalTime endTime
    ) {
        boolean duplicated = subjectRepository
            .findAllByClassroomIdAndDayOfWeekAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndIsActiveTrue(
                classroomId,
                dayOfWeek,
                endAt,
                startAt
            )
            .stream()
            .filter(candidate -> !candidate.getId().equals(subjectId))
            .anyMatch(candidate -> hasActualScheduleConflict(candidate, dayOfWeek, startAt, endAt, startTime, endTime));

        if (duplicated) {
            throw new SubjectDuplicateException("같은 분반에 실제 수업 날짜와 시간이 겹치는 과목이 존재합니다.");
        }
    }

    /** 운영 기록·결석 요청·교환 요청이 걸린 미래 수업이 있으면 자동 변경하지 않는다. */
    void validateFutureLessonsChangeable(Long subjectId, LocalDate today) {
        if (lessonProxyService.existsUnchangeableFutureActiveLessonBySubjectId(subjectId, today)) {
            throw new SubjectTeacherAssignmentConflictException("운영 기록이 있는 미래 수업은 자동 변경할 수 없습니다.");
        }
        if (absenceRequestProxyService.existsActiveAbsenceRequestByLessonIds(
            lessonProxyService.getFutureActiveLessonIdsBySubjectId(subjectId, today)
        )) {
            throw new SubjectTeacherAssignmentConflictException("결석 요청이 연결된 미래 수업은 자동 변경할 수 없습니다.");
        }
        if (lessonExchangeRequestProxyService.existsActiveExchangeByLessonTeacherDates(
            lessonProxyService.getFutureActiveLessonTeacherDatesBySubjectId(subjectId, today)
        )) {
            throw new SubjectTeacherAssignmentConflictException("수업 교환 요청 또는 제안이 연결된 미래 수업은 자동 변경할 수 없습니다.");
        }
    }

    /** 새 담당 교사가 과목의 미래 수업을 지금 시간 그대로 맡을 수 있는지. */
    void validateNoTeacherConflict(Long subjectId, Long teacherId, LocalDate today) {
        if (lessonProxyService.existsTeacherConflictForFutureSubjectScheduledLessons(subjectId, teacherId, today, null, null)) {
            throw new SubjectTeacherAssignmentConflictException("새 담당 교사의 기존 수업과 시간이 겹쳐 자동 변경할 수 없습니다.");
        }
    }

    /**
     * 담당 교사가 바뀐 일정을 맡을 수 있는지.
     * 수업을 다시 만드는 변경이면 새 기간의 날짜로, 시간만 바꾸면 지금 미래 수업의 날짜로 본다.
     */
    void validateNoTeacherConflictForSchedule(
        Long subjectId,
        Long teacherId,
        LocalDate today,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean recreateLessons
    ) {
        boolean conflict = recreateLessons
            ? lessonProxyService.existsTeacherConflictForSubjectSchedule(
                subjectId, teacherId, startAt, endAt, dayOfWeek, startTime, endTime
            )
            : lessonProxyService.existsTeacherConflictForFutureSubjectScheduledLessons(
                subjectId, teacherId, today, startTime, endTime
            );

        if (conflict) {
            throw new SubjectTeacherAssignmentConflictException("변경할 일정이 담당 교사의 기존 수업과 겹쳐 자동 변경할 수 없습니다.");
        }
    }

    /** 교사는 운영기간이 겹치는 동안 하루치 일정(분반·요일·기간) 하나만 맡는다 (#199). */
    void validateTeacherScheduleAssignable(Long teacherId, Subject subject) {
        if (subjectRepository.existsOverlappingDifferentScheduleByTeacherId(
            teacherId,
            subject.getClassroom().getId(),
            subject.getDayOfWeek(),
            subject.getStartAt(),
            subject.getEndAt()
        )) {
            throw new SubjectTeacherAssignmentConflictException("이미 다른 하루치 일정의 활성 과목을 담당 중인 교사입니다.");
        }
    }

    private boolean hasActualScheduleConflict(
        Subject candidate,
        DayOfWeek dayOfWeek,
        LocalDate startAt,
        LocalDate endAt,
        LocalTime startTime,
        LocalTime endTime
    ) {
        LocalDate overlapStart = candidate.getStartAt().isAfter(startAt) ? candidate.getStartAt() : startAt;
        LocalDate overlapEnd = candidate.getEndAt().isBefore(endAt) ? candidate.getEndAt() : endAt;
        LocalDate firstLessonDate = overlapStart.with(TemporalAdjusters.nextOrSame(dayOfWeek));

        boolean hasCommonLessonDate = !firstLessonDate.isAfter(overlapEnd);
        boolean timeOverlaps = candidate.getStartTime().isBefore(endTime)
            && candidate.getEndTime().isAfter(startTime);
        return hasCommonLessonDate && timeOverlaps;
    }
}
