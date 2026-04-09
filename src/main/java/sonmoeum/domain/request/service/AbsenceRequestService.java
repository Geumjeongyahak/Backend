package sonmoeum.domain.request.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.event.EventPublisher;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.service.LessonProxyService;
import sonmoeum.domain.request.entity.AbsenceRequest;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.event.AbsenceApprovedEvent;
import sonmoeum.domain.request.exception.RequestAlreadyProcessedException;
import sonmoeum.domain.request.exception.RequestForbiddenException;
import sonmoeum.domain.request.exception.RequestNotFoundException;
import sonmoeum.domain.request.repository.AbsenceRequestRepository;
import sonmoeum.domain.request.v1.dto.request.CreateAbsenceRequestRequest;
import sonmoeum.domain.request.v1.dto.response.AbsenceRequestResponse;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbsenceRequestService {

    private final AbsenceRequestRepository absenceRequestRepository;
    private final LessonProxyService lessonProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;


    @Transactional
    public AbsenceRequestResponse createAbsenceRequest(Long requesterId, CreateAbsenceRequestRequest request) {
        log.debug("결석 요청 생성 (requesterId={}, lessonId={})", requesterId, request.lessonId());

        Lesson lesson = lessonProxyService.getActiveById(request.lessonId());
        User requester = userProxyService.getById(requesterId);

        AbsenceRequest absenceRequest = new AbsenceRequest(lesson, requester, request.reason());
        AbsenceRequest saved = absenceRequestRepository.save(absenceRequest);

        log.debug("결석 요청 생성 완료 (id={})", saved.getId());
        return AbsenceRequestResponse.from(saved);
    }

    public List<AbsenceRequestResponse> getAbsenceRequests(Long requesterId, boolean isAdmin, RequestStatus status) {
        log.debug("결석 요청 목록 조회 (isAdmin={}, status={})", isAdmin, status);

        List<AbsenceRequest> list;
        if (status != null) {
            list = isAdmin
                ? absenceRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                : absenceRequestRepository.findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
                    status, requesterId
                );
        } else {
            list = isAdmin
                ? absenceRequestRepository.findAllByOrderByCreatedAtDesc()
                : absenceRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId);
        }

        return list.stream().map(AbsenceRequestResponse::from).toList();
    }

    public AbsenceRequestResponse getAbsenceRequest(Long requesterId, Long requestId, boolean isAdmin) {
        log.debug("결석 요청 상세 조회 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !absenceRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public AbsenceRequestResponse approveAbsenceRequest(Long approverId, Long requestId) {
        log.debug("결석 요청 승인 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);
        absenceRequest.approve(approver);

        eventPublisher.publish(new AbsenceApprovedEvent(
            absenceRequest.getLesson().getId(),
            approverId
        ));

        log.debug("결석 요청 승인 완료 (requestId={})", requestId);
        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public AbsenceRequestResponse rejectAbsenceRequest(Long approverId, Long requestId, String note) {
        log.debug("결석 요청 반려 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);
        absenceRequest.reject(approver, note);

        log.debug("결석 요청 반려 완료 (requestId={})", requestId);
        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public void deleteAbsenceRequest(Long requesterId, Long requestId, boolean isAdmin) {
        log.debug("결석 요청 삭제 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !absenceRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        absenceRequestRepository.delete(absenceRequest);
        log.debug("결석 요청 삭제 완료 (requestId={})", requestId);
    }
}
