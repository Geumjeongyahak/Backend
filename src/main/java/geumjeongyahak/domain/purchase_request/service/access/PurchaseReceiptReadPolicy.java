package geumjeongyahak.domain.purchase_request.service.access;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.service.access.AttachmentReadPolicy;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestPaymentTransactionRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestProposalReceiptRepository;
import lombok.RequiredArgsConstructor;

/**
 * 구매 영수증(품의 영수증·결제 영수증): 그 구매 요청을 열람할 수 있는 사람.
 * 두 상세 조회 API의 권한을 합친 것과 같아야 한다 —
 * {@code PurchaseRequestController.TEACHER_OR_HIGHER_ACCESS}(VOLUNTEER·MANAGER·ADMIN)와
 * {@code PurchaseRequestAdminController} 상세({@code purchase-request:read:*}).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseReceiptReadPolicy implements AttachmentReadPolicy {

    private static final Set<String> READERS = Set.of("ROLE_VOLUNTEER", "ROLE_MANAGER", "ROLE_ADMIN", "purchase-request:read:*");

    private final PurchaseRequestProposalReceiptRepository proposalReceiptRepository;
    private final PurchaseRequestPaymentTransactionRepository paymentTransactionRepository;

    @Override
    public boolean canRead(UUID fileId, CustomUserDetails user) {
        return user != null
            && user.getAuthorities().stream().anyMatch(a -> READERS.contains(a.getAuthority()))
            && (proposalReceiptRepository.existsByFileIdAndIsDeletedFalseAndProposalPurchaseRequestIsDeletedFalse(fileId)
                || paymentTransactionRepository.existsByReceiptFileIdAndPurchaseRequestIsDeletedFalse(fileId));
    }
}
