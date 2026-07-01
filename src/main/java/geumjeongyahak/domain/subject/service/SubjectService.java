package geumjeongyahak.domain.subject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.exception.ClassroomNotFoundException;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.request.service.AbsenceRequestProxyService;
import geumjeongyahak.domain.request.service.LessonExchangeRequestProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.SubjectDuplicateException;
import geumjeongyahak.domain.subject.exception.SubjectNotFoundException;
import geumjeongyahak.domain.subject.exception.SubjectOperationPeriodExceededException;
import geumjeongyahak.domain.subject.exception.SubjectTeacherAssignmentConflictException;
import geumjeongyahak.domain.subject.event.SubjectCreatedEvent;
import geumjeongyahak.domain.subject.event.SubjectScheduleRecreatedEvent;
import geumjeongyahak.domain.subject.event.SubjectScheduleUpdatedEvent;
import geumjeongyahak.domain.subject.event.SubjectTeacherAssignedEvent;
import geumjeongyahak.domain.subject.event.SubjectTeacherUnassignedEvent;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.subject.v1.dto.request.AssignSubjectTeacherRequest;
import geumjeongyahak.domain.subject.v1.dto.request.CreateSubjectRequest;
import geumjeongyahak.domain.subject.v1.dto.request.UpdateSubjectBasicRequest;
import geumjeongyahak.domain.subject.v1.dto.request.UpdateSubjectScheduleRequest;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {

    private static final long MAX_SUBJECT_OPERATION_DAYS = 365;

    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final UserProxyService userProxyService;
    private final LessonProxyService lessonProxyService;
    private final AbsenceRequestProxyService absenceRequestProxyService;
    private final LessonExchangeRequestProxyService lessonExchangeRequestProxyService;
    private final EventPublisher eventPublisher;

    @Transactional
    public SubjectDetailResponse createSubject(CreateSubjectRequest request) {
        log.debug("과목 등록 요청 (name={})", request.name());
        validateCreateSchedule(request.startAt(), request.endAt(), request.startTime(), request.endTime());

        Classroom classroom = classroomRepository.findById(request.classroomId())
            .orElseThrow(() -> {
                log.info("과목 등록 실패 - 교실을 찾을 수 없습니다. ID: {}", request.classroomId());
                return new ClassroomNotFoundException(request.classroomId());
            });
        User teacher = null;
        if (request.teacherId() != null) {
            teacher = userProxyService.getById(request.teacherId());
            validateTeacherAssignable(teacher);
            userProxyService.fillDefaultClassroomIfMissing(teacher, classroom);
        }

        // 같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재하는지 확인
        if (subjectRepository.existsByClassroomIdAndDayOfWeekAndPeriodAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            classroom.getId(),
            request.dayOfWeek(),
            request.period(),
            request.endAt(),
            request.startAt()
        )) {
            log.info("과목 등록 실패 - 같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재합니다.");
            throw new SubjectDuplicateException("같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재합니다.");
        }

        Subject subject = new Subject(
            classroom,
            teacher,
            request.name(),
            request.startAt(),
            request.endAt(),
            request.dayOfWeek(),
            request.startTime(),
            request.endTime(),
            request.period(),
            teacher != null ? LocalDateTime.now() : null,
            request.description()
        );

        Subject savedSubject = subjectRepository.save(subject);
        log.debug("과목 등록 완료 (id={})", savedSubject.getId());

        if (savedSubject.getTeacher() != null) {
            LocalDate lessonStartAt = max(LocalDate.now(), savedSubject.getStartAt());
            if (!lessonStartAt.isAfter(savedSubject.getEndAt())) {
                eventPublisher.publish(new SubjectCreatedEvent(
                    savedSubject.getId(),
                    savedSubject.getTeacher().getId(),
                    lessonStartAt,
                    savedSubject.getEndAt(),
                    savedSubject.getDayOfWeek(),
                    savedSubject.getStartTime(),
                    savedSubject.getEndTime(),
                    savedSubject.getPeriod()
                ));
            }
        }

        return SubjectDetailResponse.from(savedSubject);
    }

    public SubjectDetailResponse getSubject(Long subjectId) {
        log.debug("과목 단건 조회 요청 (id={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 단건 조회 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });

        log.debug("과목 단건 조회 완료 (id={})", subject.getId());
        return SubjectDetailResponse.from(subject);
    }

    public List<SubjectDetailResponse> getAllSubjects(Long classroomId) {
        log.debug("과목 목록 조회 요청 (classroomId={})", classroomId);

        if (classroomId != null) {
            classroomRepository.findById(classroomId)
                .orElseThrow(() -> {
                    log.info("과목 목록 조회 실패 - 분반을 찾을 수 없습니다. ID: {}", classroomId);
                    return new ClassroomNotFoundException(classroomId);
                });

            return subjectRepository.findByClassroomId(classroomId).stream()
                .map(SubjectDetailResponse::from)
                .toList();
        }

        return subjectRepository.findAll().stream()
            .map(SubjectDetailResponse::from)
            .toList();
    }

    public List<SubjectDetailResponse> getUnassignedSubjects() {
        log.debug("교사 미배정 과목 목록 조회 요청");

        List<SubjectDetailResponse> responses = subjectRepository
            .findAllByTeacherIsNullAndIsActiveTrueOrderByStartAtAscIdAsc()
            .stream()
            .map(SubjectDetailResponse::from)
            .toList();
        log.debug("교사 미배정 과목 목록 조회 완료 - 총 {}건", responses.size());
        return responses;
    }

    public List<SubjectDetailResponse> getMyAssignedSubjects(Long teacherId) {
        log.debug("내 담당 과목 목록 조회 요청 (teacherId={})", teacherId);

        return subjectRepository.findAllByTeacherIdAndIsActiveTrueOrderByStartAtAscIdAsc(teacherId)
            .stream()
            .map(SubjectDetailResponse::from)
            .toList();
    }

    @Transactional
    public SubjectDetailResponse updateSubject(Long subjectId, UpdateSubjectBasicRequest request) {
        log.debug("과목 기본 정보 수정 요청 (id={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 수정 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });

        subject.updateBasic(request.getName(), request.getDescription());

        Subject saved = subjectRepository.save(subject);
        log.debug("과목 기본 정보 수정 완료 (id={})", saved.getId());
        return SubjectDetailResponse.from(saved);
    }

    @Transactional
    public SubjectDetailResponse assignTeacher(Long subjectId, AssignSubjectTeacherRequest request) {
        log.debug("과목 담당 교사 배정 요청 (subjectId={}, teacherId={})", subjectId, request.teacherId());

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 담당 교사 배정 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });
        LocalDate today = LocalDate.now();
        if (request.teacherId() == null) {
            validateFutureLessonsChangeable(subjectId, today);
            subject.assignTeacher(null, null);
            eventPublisher.publish(new SubjectTeacherUnassignedEvent(subject.getId(), today));
            log.debug("과목 담당 교사 해제 완료 (subjectId={})", subject.getId());
            return SubjectDetailResponse.from(subject);
        }

        User teacher = userProxyService.getById(request.teacherId());
        validateTeacherAssignable(teacher);

        validateFutureLessonsChangeable(subjectId, today);
        validateNoTeacherConflict(subjectId, teacher.getId(), today);
        userProxyService.fillDefaultClassroomIfMissing(teacher, subject.getClassroom());

        subject.assignTeacher(teacher, LocalDateTime.now());

        if (lessonProxyService.existsFutureActiveLessonBySubjectId(subjectId, today)) {
            eventPublisher.publish(new SubjectTeacherAssignedEvent(
                subject.getId(),
                teacher.getId(),
                today
            ));
        } else {
            LocalDate lessonStartAt = max(today, subject.getStartAt());
            if (!lessonStartAt.isAfter(subject.getEndAt())) {
                eventPublisher.publish(new SubjectCreatedEvent(
                    subject.getId(),
                    teacher.getId(),
                    lessonStartAt,
                    subject.getEndAt(),
                    subject.getDayOfWeek(),
                    subject.getStartTime(),
                    subject.getEndTime(),
                    subject.getPeriod()
                ));
            }
        }

        log.debug("과목 담당 교사 배정 완료 (subjectId={}, teacherId={})", subject.getId(), teacher.getId());
        return SubjectDetailResponse.from(subject);
    }

    @Transactional
    public SubjectDetailResponse updateSchedule(Long subjectId, UpdateSubjectScheduleRequest request) {
        log.debug("과목 일정 수정 요청 (subjectId={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 일정 수정 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });

        LocalDate newStartAt = request.startAt() != null ? request.startAt() : subject.getStartAt();
        LocalDate newEndAt = request.endAt() != null ? request.endAt() : subject.getEndAt();
        var newDayOfWeek = request.dayOfWeek() != null ? request.dayOfWeek() : subject.getDayOfWeek();
        var newStartTime = request.startTime() != null ? request.startTime() : subject.getStartTime();
        var newEndTime = request.endTime() != null ? request.endTime() : subject.getEndTime();
        Integer newPeriod = request.period() != null ? request.period() : subject.getPeriod();

        validateSchedule(newStartAt, newEndAt, newStartTime, newEndTime);
        validateSubjectDuplicate(
            subject.getId(),
            subject.getClassroom().getId(),
            newDayOfWeek,
            newPeriod,
            newStartAt,
            newEndAt
        );

        boolean recreateLessons = isChanged(subject.getStartAt(), newStartAt)
            || isChanged(subject.getEndAt(), newEndAt)
            || isChanged(subject.getDayOfWeek(), newDayOfWeek);
        boolean updateLessons = recreateLessons
            || isChanged(subject.getStartTime(), newStartTime)
            || isChanged(subject.getEndTime(), newEndTime)
            || isChanged(subject.getPeriod(), newPeriod);

        LocalDate today = LocalDate.now();
        Long teacherId = subject.getTeacher() != null ? subject.getTeacher().getId() : null;
        if (updateLessons && teacherId != null) {
            validateFutureLessonsChangeable(subjectId, today);
            validateNoTeacherConflictForSchedule(
                subjectId,
                teacherId,
                max(today, newStartAt),
                newEndAt,
                newDayOfWeek,
                newStartTime,
                newEndTime,
                recreateLessons
            );
        }

        subject.updateSchedule(
            newStartAt,
            newEndAt,
            newDayOfWeek,
            newStartTime,
            newEndTime,
            newPeriod
        );

        if (updateLessons && teacherId != null) {
            if (recreateLessons) {
                eventPublisher.publish(new SubjectScheduleRecreatedEvent(
                    subject.getId(),
                    teacherId,
                    today,
                    max(today, newStartAt),
                    newEndAt,
                    newDayOfWeek,
                    newStartTime,
                    newEndTime,
                    newPeriod
                ));
            } else {
                eventPublisher.publish(new SubjectScheduleUpdatedEvent(
                    subject.getId(),
                    today,
                    newStartTime,
                    newEndTime,
                    newPeriod
                ));
            }
        }

        log.debug("과목 일정 수정 완료 (subjectId={})", subject.getId());
        return SubjectDetailResponse.from(subject);
    }

    @Transactional
    public void deleteSubject(Long subjectId) {
        log.debug("과목 삭제(비활성화) 요청 (id={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 삭제 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });

        // 이미 비활성화면 멱등하게 처리(성공 처리)
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            log.debug("과목은 이미 비활성화 상태입니다. (id={})", subjectId);
            return;
        }

        subject.deactivate();
        subjectRepository.save(subject);

        log.debug("과목 삭제(비활성화) 완료 (id={})", subjectId);
    }

    private void validateFutureLessonsChangeable(Long subjectId, LocalDate today) {
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

    private void validateNoTeacherConflict(
        Long subjectId,
        Long teacherId,
        LocalDate today
    ) {
        if (lessonProxyService.existsTeacherConflictForFutureSubjectScheduledLessons(
            subjectId,
            teacherId,
            today
        )) {
            throw new SubjectTeacherAssignmentConflictException("새 담당 교사의 기존 수업과 시간이 겹쳐 자동 변경할 수 없습니다.");
        }
    }

    private void validateNoTeacherConflictForSchedule(
        Long subjectId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        java.time.DayOfWeek dayOfWeek,
        java.time.LocalTime startTime,
        java.time.LocalTime endTime,
        boolean recreateLessons
    ) {
        boolean conflict;
        if (recreateLessons) {
            conflict = !startAt.isAfter(endAt)
                && lessonProxyService.existsTeacherConflictForSubjectSchedule(
                    subjectId,
                    teacherId,
                    startAt,
                    endAt,
                    dayOfWeek,
                    startTime,
                    endTime
                );
        } else {
            conflict = lessonProxyService.existsTeacherConflictForFutureSubjectScheduledLessons(
                subjectId,
                teacherId,
                LocalDate.now(),
                startTime,
                endTime
            );
        }

        if (conflict) {
            throw new SubjectTeacherAssignmentConflictException("변경할 일정이 담당 교사의 기존 수업과 겹쳐 자동 변경할 수 없습니다.");
        }
    }

    private void validateSchedule(
        LocalDate startAt,
        LocalDate endAt,
        java.time.LocalTime startTime,
        java.time.LocalTime endTime
    ) {
        if (startAt.isAfter(endAt)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "startAt은 endAt보다 늦을 수 없습니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "startTime은 endTime보다 빨라야 합니다.");
        }
    }

    private void validateCreateSchedule(
        LocalDate startAt,
        LocalDate endAt,
        java.time.LocalTime startTime,
        java.time.LocalTime endTime
    ) {
        validateSchedule(startAt, endAt, startTime, endTime);

        long operationDays = ChronoUnit.DAYS.between(startAt, endAt) + 1;
        if (operationDays > MAX_SUBJECT_OPERATION_DAYS) {
            throw new SubjectOperationPeriodExceededException(MAX_SUBJECT_OPERATION_DAYS);
        }
    }

    private void validateSubjectDuplicate(
        Long subjectId,
        Long classroomId,
        java.time.DayOfWeek dayOfWeek,
        Integer period,
        LocalDate startAt,
        LocalDate endAt
    ) {
        if (subjectRepository.existsByIdNotAndClassroomIdAndDayOfWeekAndPeriodAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            subjectId,
            classroomId,
            dayOfWeek,
            period,
            endAt,
            startAt
        )) {
            throw new SubjectDuplicateException("같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재합니다.");
        }
    }

    private boolean isChanged(Object before, Object after) {
        return !Objects.equals(before, after);
    }

    private LocalDate max(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private void validateTeacherAssignable(User teacher) {
        if (teacher.getRole() != RoleType.VOLUNTEER
            && teacher.getRole() != RoleType.MANAGER
            && teacher.getRole() != RoleType.ADMIN) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "봉사자, 매니저 또는 관리자 사용자만 교사로 배정할 수 있습니다.");
        }
    }
}
