package geumjeongyahak.domain.purchase_request.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.service.FileProxyService;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposal;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalApprovalLine;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalBudget;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalReceipt;
import geumjeongyahak.domain.purchase_request.enums.PurchaseDocumentApprovalType;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestProposalRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestProposalReceiptRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest.ApprovalLineRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest.BudgetRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestProposalResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private final PurchaseRequestProposalReceiptRepository receiptRepository;
    private final DepartmentProxyService departmentProxyService;
    private final FileProxyService fileProxyService;

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
            normalize(request.proposalTitle()),
            normalize(request.resolutionTitle()),
            request.completionDate(),
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
        proposal.replaceApprovalLines(toApprovalLines(request));

        PurchaseRequestProposal saved = proposalRepository.saveAndFlush(proposal);
        log.debug("품의 정보 저장 완료 (requestId={}, proposalId={})", requestId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseRequestProposalResponse attachReceipt(
        Long actorId,
        Long requestId,
        UUID fileId,
        boolean isAdmin
    ) {
        PurchaseRequest purchaseRequest = findPurchaseRequest(requestId);
        checkAccess(purchaseRequest, actorId, isAdmin);
        validateEditable(purchaseRequest);

        File file = fileProxyService.getActiveById(fileId);
        PurchaseRequestProposal proposal = purchaseRequest.getProposal() != null
            ? purchaseRequest.getProposal()
            : new PurchaseRequestProposal(purchaseRequest);

        boolean alreadyAttached = proposal.getReceipts().stream()
            .anyMatch(receipt -> !receipt.isDeleted() && receipt.getFile().getId().equals(fileId));
        if (!alreadyAttached) {
            proposal.addReceipt(new PurchaseRequestProposalReceipt(file));
        }

        PurchaseRequestProposal saved = proposalRepository.saveAndFlush(proposal);
        log.debug("품의 단계 영수증 첨부 완료 (requestId={}, fileId={})", requestId, fileId);
        return toResponse(saved);
    }

    @Transactional
    public void deleteReceipt(
        Long actorId,
        Long requestId,
        Long receiptId,
        boolean isAdmin
    ) {
        PurchaseRequest purchaseRequest = findPurchaseRequest(requestId);
        checkAccess(purchaseRequest, actorId, isAdmin);
        validateEditable(purchaseRequest);

        PurchaseRequestProposalReceipt receipt = receiptRepository
            .findByIdAndProposal_PurchaseRequest_IdAndIsDeletedFalse(receiptId, requestId)
            .orElseThrow(() -> new ResourceNotFoundException(
                PurchaseRequestErrorCode.PROPOSAL_RECEIPT_NOT_FOUND,
                receiptId
            ));
        receipt.softDelete();
        log.debug(
            "품의 단계 영수증 삭제 완료 (requestId={}, receiptId={}, fileId={})",
            requestId,
            receiptId,
            receipt.getFile().getId()
        );
    }

    public void validateForConfirmation(PurchaseRequest purchaseRequest) {
        PurchaseRequestProposal proposal = purchaseRequest.getProposal();
        if (proposal == null) {
            throw new BusinessException(PurchaseRequestErrorCode.PROPOSAL_REQUIRED);
        }
        if (proposal.getProposalDate() == null
            || proposal.getPaymentAccount() == null
            || proposal.getProposalAmount() == null
            || proposal.getCompletionDate() == null
            || proposal.getItems().isEmpty()
            || proposal.getItems().stream().anyMatch(item ->
                item.getQuantity() == null || item.getEstimatedUnitPrice() == null)) {
            throw new BusinessException(PurchaseRequestErrorCode.PROPOSAL_REQUIRED_FIELD_MISSING);
        }

        long expectedAmountTotal;
        try {
            expectedAmountTotal = proposal.getItems().stream()
                .map(PurchaseRequestProposalItem::calculateExpectedAmount)
                .reduce(0L, Math::addExact);
        } catch (ArithmeticException exception) {
            throw new BusinessException(PurchaseRequestErrorCode.PROPOSAL_ITEM_AMOUNT_MISMATCH);
        }

        if (proposal.getProposalAmount() != expectedAmountTotal) {
            throw new BusinessException(PurchaseRequestErrorCode.PROPOSAL_ITEM_AMOUNT_MISMATCH);
        }
        if (!proposal.getProposalAmount().equals(purchaseRequest.getTotalPrice())) {
            throw new BusinessException(PurchaseRequestErrorCode.PROPOSAL_PAYMENT_AMOUNT_MISMATCH);
        }
    }

    public boolean hasActiveReceipt(PurchaseRequest purchaseRequest) {
        return purchaseRequest.getProposal() != null
            && purchaseRequest.getProposal().getReceipts().stream()
                .anyMatch(receipt -> !receipt.isDeleted() && !receipt.getFile().isDeleted());
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

    public String calculateProposalNumber(PurchaseRequestProposal proposal) {
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

    private List<PurchaseRequestProposalApprovalLine> toApprovalLines(
        SavePurchaseRequestProposalRequest request
    ) {
        List<PurchaseRequestProposalApprovalLine> result = new ArrayList<>();
        addApprovalLines(result, PurchaseDocumentApprovalType.DRAFT_APPROVAL, request.draftApprovals());
        addApprovalLines(result, PurchaseDocumentApprovalType.DRAFT_COOPERATION, request.draftCooperations());
        addApprovalLines(result, PurchaseDocumentApprovalType.RESOLUTION_APPROVAL, request.resolutionApprovals());
        return result;
    }

    private void addApprovalLines(
        List<PurchaseRequestProposalApprovalLine> result,
        PurchaseDocumentApprovalType lineType,
        List<ApprovalLineRequest> lines
    ) {
        if (lines == null) {
            return;
        }
        for (int index = 0; index < lines.size(); index++) {
            ApprovalLineRequest line = lines.get(index);
            result.add(new PurchaseRequestProposalApprovalLine(
                lineType,
                normalize(line.position()),
                normalize(line.name()),
                index
            ));
        }
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
