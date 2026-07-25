package geumjeongyahak.domain.purchase_request.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposal;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalBudget;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalItem;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestProposalRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest.BudgetRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestProposalResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestProposalService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestProposalRepository proposalRepository;
    private final DepartmentProxyService departmentProxyService;

    @Transactional
    public PurchaseRequestProposalResponse saveProposal(
        Long actorId,
        Long requestId,
        SavePurchaseRequestProposalRequest request,
        boolean isAdmin
    ) {
        PurchaseRequest purchaseRequest = findPurchaseRequest(requestId);
        checkAccess(purchaseRequest, actorId, isAdmin);
        validateEditable(purchaseRequest);

        PurchaseRequestProposal proposal = purchaseRequest.getProposal() != null
            ? purchaseRequest.getProposal()
            : new PurchaseRequestProposal(purchaseRequest);
        Department requestDepartment = request.requestDepartmentId() != null
            ? departmentProxyService.getById(request.requestDepartmentId())
            : null;

        proposal.updateDetails(
            normalize(request.overview()),
            normalize(request.policyProject()),
            normalize(request.unitProject()),
            normalize(request.detailProject()),
            requestDepartment,
            request.proposalDate(),
            request.proposalAmount(),
            request.paymentAccount()
        );
        updateBudget(proposal, request.budget());
        proposal.replaceItems(toItems(request.items()));

        PurchaseRequestProposal saved = proposalRepository.saveAndFlush(proposal);
        log.debug("품의 정보 저장 완료 (requestId={}, proposalId={})", requestId, saved.getId());
        return toResponse(saved);
    }

    public PurchaseRequestProposalResponse toResponse(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getProposal() == null) {
            return null;
        }
        return toResponse(purchaseRequest.getProposal());
    }

    private PurchaseRequestProposalResponse toResponse(PurchaseRequestProposal proposal) {
        return PurchaseRequestProposalResponse.from(proposal, calculateProposalNumber(proposal));
    }

    private String calculateProposalNumber(PurchaseRequestProposal proposal) {
        if (proposal.getProposalDate() == null
            || proposal.getPaymentAccount() == null
            || proposal.getPurchaseRequest().isDeleted()
            || proposal.getPurchaseRequest().getStatus() == PurchaseRequestStatus.REJECTED) {
            return null;
        }

        LocalDate yearStart = LocalDate.of(proposal.getProposalDate().getYear(), 1, 1);
        long sequence = proposalRepository.countNumberedRequestsThrough(
            proposal.getPaymentAccount(),
            yearStart,
            yearStart.plusYears(1),
            proposal.getProposalDate(),
            proposal.getPurchaseRequest().getCreatedAt(),
            proposal.getPurchaseRequest().getId(),
            PurchaseRequestStatus.REJECTED
        );
        return "%d품-%s-%02d".formatted(
            proposal.getProposalDate().getYear(),
            proposal.getPaymentAccount().getDisplayName(),
            sequence
        );
    }

    private void updateBudget(PurchaseRequestProposal proposal, BudgetRequest request) {
        if (request == null) {
            proposal.replaceBudget(null);
            return;
        }

        String customItemCategory = normalize(request.customItemCategory());
        String customCalculationDetail = normalize(request.customCalculationDetail());
        if (proposal.getBudget() == null) {
            proposal.replaceBudget(new PurchaseRequestProposalBudget(
                request.itemCategory(),
                customItemCategory,
                request.calculationDetail(),
                customCalculationDetail
            ));
            return;
        }

        proposal.getBudget().updateDetails(
            request.itemCategory(),
            customItemCategory,
            request.calculationDetail(),
            customCalculationDetail
        );
    }

    private List<PurchaseRequestProposalItem> toItems(
        List<SavePurchaseRequestProposalRequest.ItemRequest> requests
    ) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
            .map(item -> new PurchaseRequestProposalItem(
                normalize(item.content()),
                normalize(item.specification()),
                item.quantity(),
                item.estimatedUnitPrice()
            ))
            .toList();
    }

    private PurchaseRequest findPurchaseRequest(Long requestId) {
        return purchaseRequestRepository.findByIdAndIsDeletedFalse(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, requestId));
    }

    private void checkAccess(PurchaseRequest purchaseRequest, Long actorId, boolean isAdmin) {
        if (!isAdmin && !purchaseRequest.getRequestedBy().getId().equals(actorId)) {
            throw new BusinessException(PurchaseRequestErrorCode.FORBIDDEN);
        }
    }

    private void validateEditable(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getStatus() == PurchaseRequestStatus.CONFIRMED
            || purchaseRequest.getStatus() == PurchaseRequestStatus.REJECTED) {
            throw new BusinessException(PurchaseRequestErrorCode.PROPOSAL_NOT_EDITABLE);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
