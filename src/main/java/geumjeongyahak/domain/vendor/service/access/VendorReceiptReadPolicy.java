package geumjeongyahak.domain.vendor.service.access;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.service.access.AttachmentReadPolicy;
import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import lombok.RequiredArgsConstructor;

/**
 * 거래처 잔액 이력 영수증: 구매 영수증과 같은 규칙으로 교사 이상(VOLUNTEER·MANAGER·ADMIN).
 * {@code PurchaseRequestController.TEACHER_OR_HIGHER_ACCESS}
 * ({@code hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')})와 같아야 한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorReceiptReadPolicy implements AttachmentReadPolicy {

    private static final Set<String> TEACHER_OR_HIGHER_ROLES = Set.of("ROLE_VOLUNTEER", "ROLE_MANAGER", "ROLE_ADMIN");

    private final VendorBalanceHistoryRepository vendorBalanceHistoryRepository;

    @Override
    public boolean canRead(UUID fileId, CustomUserDetails user) {
        return user != null
            && user.getAuthorities().stream().anyMatch(a -> TEACHER_OR_HIGHER_ROLES.contains(a.getAuthority()))
            && vendorBalanceHistoryRepository.existsByReceiptFileIdAndIsDeletedFalse(fileId);
    }
}
