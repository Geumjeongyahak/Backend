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
 * 구매 영수증(품의 영수증·결제 영수증): 교사 이상(VOLUNTEER·MANAGER·ADMIN).
 * {@code PurchaseRequestController.TEACHER_OR_HIGHER_ACCESS}
 * ({@code hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')})와 같아야 한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseReceiptReadPolicy implements AttachmentReadPolicy {

    private static final Set<String> TEACHER_OR_HIGHER_ROLES = Set.of("ROLE_VOLUNTEER", "ROLE_MANAGER", "ROLE_ADMIN");

    private final PurchaseRequestProposalReceiptRepository proposalReceiptRepository;
    private final PurchaseRequestPaymentTransactionRepository paymentTransactionRepository;

    @Override
    public boolean canRead(UUID fileId, CustomUserDetails user) {
        return user != null
            && user.getAuthorities().stream().anyMatch(a -> TEACHER_OR_HIGHER_ROLES.contains(a.getAuthority()))
            && (proposalReceiptRepository.existsByFileIdAndIsDeletedFalseAndProposalPurchaseRequestIsDeletedFalse(fileId)
                || paymentTransactionRepository.existsByReceiptFileIdAndPurchaseRequestIsDeletedFalse(fileId));
    }
}
