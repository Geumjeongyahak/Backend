package sonmoeum.domain.request.service;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.requests.dto.request.CreatePurchaseRequest;
import sonmoeum.api.v1.requests.dto.request.RequestStatusUpdateRequest;
import sonmoeum.api.v1.requests.dto.response.PurchaseRequestResponse;
import sonmoeum.domain.request.entity.PurchaseRequest;
import sonmoeum.domain.request.repository.PurchaseRequestRepository;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseRequestService {
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;


    public BasePageResponse<PurchaseRequestResponse> getPurchaseRequestPagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            purchaseRequestRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(PurchaseRequestResponse::from);
    }

    public PurchaseRequestResponse getPurchaseRequestById(Long id) {
        PurchaseRequest request = purchaseRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 요청이 존재하지 않습니다."));
        return PurchaseRequestResponse.from(request);
    }

    @Transactional
    public PurchaseRequestResponse createPurchaseRequest(Long userId, CreatePurchaseRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 과목이 존재하지 않습니다."));
        User requestedBy = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다."));

        PurchaseRequest purchaseRequest = new PurchaseRequest(
            subject,
            requestedBy,
            request.title(),
            request.content(),
            request.price()
        );
        return PurchaseRequestResponse.from(purchaseRequestRepository.save(purchaseRequest));
    }


    @Transactional
    public PurchaseRequestResponse updateStatus(Long id, Long approverId, RequestStatusUpdateRequest request) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 요청이 존재하지 않습니다."));
        
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 승인자가 존재하지 않습니다."));
        
        switch (request.status()) {
            case APPROVED -> purchaseRequest.approve(approver);
            case REJECTED -> purchaseRequest.reject(approver, request.note());
            default -> throw new IllegalArgumentException("유효하지 않은 상태 변경 요청입니다.");
        }


        return PurchaseRequestResponse.from(purchaseRequestRepository.save(purchaseRequest));
    }
}
