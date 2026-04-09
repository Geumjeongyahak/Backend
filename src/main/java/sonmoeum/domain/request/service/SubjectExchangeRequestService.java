package sonmoeum.domain.request.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.request.entity.SubjectExchangeRequest;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.exception.RequestAlreadyProcessedException;
import sonmoeum.domain.request.exception.RequestForbiddenException;
import sonmoeum.domain.request.exception.RequestNotFoundException;
import sonmoeum.common.event.EventPublisher;
import sonmoeum.domain.request.event.SubjectApprovedEvent;
import sonmoeum.domain.request.repository.SubjectExchangeRequestRepository;
import sonmoeum.domain.request.v1.dto.request.CreateSubjectExchangeRequestRequest;
import sonmoeum.domain.request.v1.dto.response.SubjectExchangeRequestResponse;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.service.SubjectProxyService;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectExchangeRequestService {

    private final SubjectExchangeRequestRepository subjectExchangeRequestRepository;
    private final SubjectProxyService subjectProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;

    @Transactional
    public SubjectExchangeRequestResponse createSubjectExchangeRequest(
        Long requesterId,
        CreateSubjectExchangeRequestRequest request
    ) {
        log.debug("과목 교환 요청 생성 (requesterId={}, subjectId={})", requesterId, request.subjectId());

        Subject subject = subjectProxyService.getById(request.subjectId());
        if (!subject.getTeacher().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
        User requester = userProxyService.getById(requesterId);

        SubjectExchangeRequest exchangeRequest = new SubjectExchangeRequest(
            subject, requester, request.title(), request.content()
        );
        SubjectExchangeRequest saved = subjectExchangeRequestRepository.save(exchangeRequest);

        log.debug("과목 교환 요청 생성 완료 (id={})", saved.getId());
        return SubjectExchangeRequestResponse.from(saved);
    }

    public List<SubjectExchangeRequestResponse> getSubjectExchangeRequests(
        Long requesterId, boolean isAdmin, RequestStatus status
    ) {
        log.debug("과목 교환 요청 목록 조회 (isAdmin={}, status={})", isAdmin, status);

        List<SubjectExchangeRequest> list;
        if (status != null) {
            list = isAdmin
                ? subjectExchangeRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                : subjectExchangeRequestRepository.findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
                    status, requesterId
                );
        } else {
            list = isAdmin
                ? subjectExchangeRequestRepository.findAllByOrderByCreatedAtDesc()
                : subjectExchangeRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId);
        }

        return list.stream().map(SubjectExchangeRequestResponse::from).toList();
    }

    public SubjectExchangeRequestResponse getSubjectExchangeRequest(
        Long requesterId, Long requestId, boolean isAdmin
    ) {
        log.debug("과목 교환 요청 상세 조회 (requestId={})", requestId);
        SubjectExchangeRequest exchangeRequest = subjectExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !exchangeRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        return SubjectExchangeRequestResponse.from(exchangeRequest);
    }

    @Transactional
    public SubjectExchangeRequestResponse approveSubjectExchangeRequest(
        Long approverId, Long requestId, Long exchangeWithUserId
    ) {
        log.debug("과목 교환 요청 승인 (requestId={}, exchangeWithUserId={})", requestId, exchangeWithUserId);
        SubjectExchangeRequest exchangeRequest = subjectExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);
        User newTeacher = userProxyService.getById(exchangeWithUserId);

        exchangeRequest.approve(approver);
        exchangeRequest.getSubject().changeTeacher(newTeacher);

        eventPublisher.publish(new SubjectApprovedEvent(
            exchangeRequest.getSubject().getId(),
            exchangeWithUserId,
            LocalDate.now()
        ));

        log.debug("과목 교환 요청 승인 완료 (requestId={})", requestId);
        return SubjectExchangeRequestResponse.from(exchangeRequest);
    }

    @Transactional
    public SubjectExchangeRequestResponse rejectSubjectExchangeRequest(
        Long approverId, Long requestId, String note
    ) {
        log.debug("과목 교환 요청 반려 (requestId={})", requestId);
        SubjectExchangeRequest exchangeRequest = subjectExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);

        exchangeRequest.reject(approver, note);

        log.debug("과목 교환 요청 반려 완료 (requestId={})", requestId);
        return SubjectExchangeRequestResponse.from(exchangeRequest);
    }
}
