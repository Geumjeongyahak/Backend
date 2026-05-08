package geumjeongyahak.domain.subject.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.exception.ClassroomNotFoundException;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.InvalidSubjectScheduleException;
import geumjeongyahak.domain.subject.exception.SubjectDuplicateException;
import geumjeongyahak.domain.subject.exception.SubjectNotFoundException;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.subject.event.SubjectCreatedEvent;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.subject.v1.dto.request.CreateSubjectRequest;
import geumjeongyahak.domain.subject.v1.dto.request.UpdateSubjectBasicRequest;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public SubjectDetailResponse createSubject(CreateSubjectRequest request) {
        log.debug("과목 등록 요청 (name={})", request.name());
        Classroom classroom = classroomRepository.findById(request.classroomId())
            .orElseThrow(() -> {
                log.info("과목 등록 실패 - 교실을 찾을 수 없습니다. ID: {}", request.classroomId());
                return new ClassroomNotFoundException(request.classroomId());
            });
        User teacher = null;
        if (request.teacherId() != null) {
            teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> {
                    log.info("과목 등록 실패 - 교사를 찾을 수 없습니다. ID: {}", request.teacherId());
                    return new UserNotFoundException(request.teacherId());
                });
            validateTeacherAssignable(teacher);
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
            request.times(),
            request.dayOfWeek(),
            request.startTime(),
            request.endTime(),
            request.period(),
            resolveAssignedFrom(teacher, request.assignedFrom(), request.startAt()),
            resolveAssignedTo(teacher, request.assignedTo(), request.endAt()),
            request.description()
        );
        validateAssignmentRange(teacher, subject.getAssignedFrom(), subject.getAssignedTo(), subject.getStartAt(), subject.getEndAt());

        Subject savedSubject = subjectRepository.save(subject);
        log.debug("과목 등록 완료 (id={})", savedSubject.getId());

        if (savedSubject.getTeacher() != null) {
            eventPublisher.publish(new SubjectCreatedEvent(
                savedSubject.getId(),
                savedSubject.getTeacher().getId(),
                savedSubject.getAssignedFrom(),
                savedSubject.getAssignedTo(),
                savedSubject.getTimes(),
                savedSubject.getDayOfWeek(),
                savedSubject.getStartTime(),
                savedSubject.getEndTime(),
                savedSubject.getPeriod()
            ));
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

    private LocalDate resolveAssignedFrom(User teacher, LocalDate assignedFrom, LocalDate startAt) {
        if (teacher == null) {
            return assignedFrom;
        }
        return assignedFrom != null ? assignedFrom : startAt;
    }

    private LocalDate resolveAssignedTo(User teacher, LocalDate assignedTo, LocalDate endAt) {
        if (teacher == null) {
            return assignedTo;
        }
        return assignedTo != null ? assignedTo : endAt;
    }

    private void validateAssignmentRange(
        User teacher,
        LocalDate assignedFrom,
        LocalDate assignedTo,
        LocalDate subjectStartAt,
        LocalDate subjectEndAt
    ) {
        if (teacher == null) {
            if (assignedFrom != null || assignedTo != null) {
                throw new InvalidSubjectScheduleException("교사가 없는 과목에는 배정 기간을 설정할 수 없습니다.");
            }
            return;
        }

        if (assignedFrom == null || assignedTo == null) {
            throw new InvalidSubjectScheduleException("교사가 배정된 과목은 assignedFrom과 assignedTo가 필요합니다.");
        }
        if (assignedFrom.isBefore(subjectStartAt) || assignedTo.isAfter(subjectEndAt)) {
            throw new InvalidSubjectScheduleException("담당 교사 배정 기간은 과목 운영 기간 안에 있어야 합니다.");
        }
    }

    private void validateTeacherAssignable(User teacher) {
        if (teacher.getRole() != RoleType.VOLUNTEER && teacher.getRole() != RoleType.MANAGER) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "봉사자 또는 매니저 사용자만 교사로 배정할 수 있습니다.");
        }
    }
}
