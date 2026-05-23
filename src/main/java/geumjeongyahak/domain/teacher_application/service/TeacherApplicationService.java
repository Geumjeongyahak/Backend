package geumjeongyahak.domain.teacher_application.service;

import java.util.List;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.teacher_application.entity.TeacherApplication;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import geumjeongyahak.domain.teacher_application.exception.DuplicatePendingTeacherApplicationException;
import geumjeongyahak.domain.teacher_application.exception.InvalidPreferredSubjectException;
import geumjeongyahak.domain.teacher_application.exception.InvalidTeacherApplicationStatusException;
import geumjeongyahak.domain.teacher_application.exception.TeacherApplicationApplicantNotGuestException;
import geumjeongyahak.domain.teacher_application.exception.TeacherApplicationForbiddenException;
import geumjeongyahak.domain.teacher_application.exception.TeacherApplicationNotFoundException;
import geumjeongyahak.domain.teacher_application.repository.TeacherApplicationRepository;
import geumjeongyahak.domain.teacher_application.v1.dto.request.CreateTeacherApplicationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.request.UpdateTeacherApplicationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.response.MyTeacherApplicationResponse;
import geumjeongyahak.domain.teacher_application.v1.dto.response.TeacherApplicationResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherApplicationService {

    private static final List<TeacherApplicationStatus> MY_VISIBLE_STATUSES = List.of(
        TeacherApplicationStatus.PENDING,
        TeacherApplicationStatus.APPROVED,
        TeacherApplicationStatus.REJECTED
    );

    private final TeacherApplicationRepository teacherApplicationRepository;
    private final UserProxyService userProxyService;
    private final SubjectProxyService subjectProxyService;

    @Transactional
    public TeacherApplicationResponse createTeacherApplication(
        Long applicantId,
        CreateTeacherApplicationRequest request
    ) {
        log.debug(
            "교원 신청 생성 요청 (applicantId={}, preferredSubjectId={})",
            applicantId,
            request.preferredSubjectId()
        );

        User applicant = userProxyService.getById(applicantId);
        validateApplicantRole(applicant);
        validateNoPendingApplication(applicantId);

        Subject preferredSubject = subjectProxyService.getById(request.preferredSubjectId());
        validatePreferredSubject(preferredSubject);

        TeacherApplication saved = teacherApplicationRepository.save(new TeacherApplication(
            applicant,
            applicant.getName(),
            request.phoneNumber(),
            request.email(),
            request.birthDate(),
            request.address(),
            request.educationAndMajor(),
            preferredSubject,
            request.motivation(),
            request.desiredTeacherImage(),
            request.meaningOfSharing()
        ));

        log.debug("교원 신청 생성 완료 (applicationId={})", saved.getId());
        return TeacherApplicationResponse.from(saved);
    }

    public MyTeacherApplicationResponse getMyTeacherApplication(Long applicantId) {
        log.debug("내 교원 신청 조회 요청 (applicantId={})", applicantId);

        return teacherApplicationRepository
            .findFirstByApplicant_IdAndStatusInOrderByCreatedAtDesc(applicantId, MY_VISIBLE_STATUSES)
            .map(TeacherApplicationResponse::from)
            .map(MyTeacherApplicationResponse::exists)
            .orElseGet(MyTeacherApplicationResponse::empty);
    }

    public TeacherApplicationResponse getTeacherApplication(Long applicationId) {
        log.debug("교원 신청 상세 조회 요청 (applicationId={})", applicationId);

        TeacherApplication application = teacherApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new TeacherApplicationNotFoundException(applicationId));

        return TeacherApplicationResponse.from(application);
    }

    @Transactional
    public TeacherApplicationResponse updateTeacherApplication(
        Long applicantId,
        Long applicationId,
        UpdateTeacherApplicationRequest request
    ) {
        log.debug(
            "교원 신청 수정 요청 (applicantId={}, applicationId={}, preferredSubjectId={})",
            applicantId,
            applicationId,
            request.preferredSubjectId()
        );

        TeacherApplication application = teacherApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new TeacherApplicationNotFoundException(applicationId));
        validateOwner(application, applicantId);
        validatePending(application);

        Subject preferredSubject = subjectProxyService.getById(request.preferredSubjectId());
        validatePreferredSubject(preferredSubject);

        application.update(
            request.phoneNumber(),
            request.email(),
            request.birthDate(),
            request.address(),
            request.educationAndMajor(),
            preferredSubject,
            request.motivation(),
            request.desiredTeacherImage(),
            request.meaningOfSharing()
        );

        log.debug("교원 신청 수정 완료 (applicationId={})", application.getId());
        return TeacherApplicationResponse.from(application);
    }

    private void validateApplicantRole(User applicant) {
        if (applicant.getRole() != RoleType.GUEST) {
            throw new TeacherApplicationApplicantNotGuestException();
        }
    }

    private void validateNoPendingApplication(Long applicantId) {
        if (teacherApplicationRepository.existsByApplicant_IdAndStatus(
            applicantId,
            TeacherApplicationStatus.PENDING
        )) {
            throw new DuplicatePendingTeacherApplicationException();
        }
    }

    private void validatePreferredSubject(Subject preferredSubject) {
        if (!Boolean.TRUE.equals(preferredSubject.getIsActive()) || preferredSubject.getTeacher() != null) {
            throw new InvalidPreferredSubjectException();
        }
    }

    private void validateOwner(TeacherApplication application, Long applicantId) {
        if (!application.getApplicant().getId().equals(applicantId)) {
            throw new TeacherApplicationForbiddenException();
        }
    }

    private void validatePending(TeacherApplication application) {
        if (application.getStatus() != TeacherApplicationStatus.PENDING) {
            throw new InvalidTeacherApplicationStatusException();
        }
    }
}
