package sonmoeum.domain.request.service;

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
import sonmoeum.domain.request.repository.SubjectExchangeRequestRepository;
import sonmoeum.domain.request.v1.dto.request.CreateSubjectExchangeRequestRequest;
import sonmoeum.domain.request.v1.dto.response.SubjectExchangeRequestResponse;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.exception.SubjectNotFoundException;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectExchangeRequestService {

    private final SubjectExchangeRequestRepository subjectExchangeRequestRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubjectExchangeRequestResponse createSubjectExchangeRequest(
        Long requesterId,
        CreateSubjectExchangeRequestRequest request
    ) {
        log.debug("과목 교환 요청 생성 (requesterId={}, subjectId={})", requesterId, request.subjectId());

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));

        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new UserNotFoundException(requesterId));

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
                : subjectExchangeRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                    .stream()
                    .filter(r -> r.getRequestedBy().getId().equals(requesterId))
                    .toList();
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
    public SubjectExchangeRequestResponse approveSubjectExchangeRequest(Long approverId, Long requestId) {
        log.debug("과목 교환 요청 승인 (requestId={})", requestId);
        SubjectExchangeRequest exchangeRequest = subjectExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new UserNotFoundException(approverId));

        exchangeRequest.approve(approver);

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

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new UserNotFoundException(approverId));

        exchangeRequest.reject(approver, note);

        log.debug("과목 교환 요청 반려 완료 (requestId={})", requestId);
        return SubjectExchangeRequestResponse.from(exchangeRequest);
    }
}
