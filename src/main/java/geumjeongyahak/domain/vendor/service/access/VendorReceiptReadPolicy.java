package geumjeongyahak.domain.vendor.service.access;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.service.access.AttachmentReadPolicy;
import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import lombok.RequiredArgsConstructor;

/**
 * 거래처 잔액 이력 영수증: 그 이력을 볼 수 있는 사람(관리자 또는 {@code vendor:read:*})만 받는다.
 * 이력 조회 API({@code GET /admin/vendors/{id}/histories})의 권한과 같아야 한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorReceiptReadPolicy implements AttachmentReadPolicy {

    // VendorAdminController#getHistories 의 @PreAuthorize("hasRole('ADMIN') or hasAuthority('vendor:read:*')")와 같아야 한다.
    // 관리자는 AttachmentUploadService 가 먼저 허용하므로 여기서는 권한 코드만 본다
    private static final String VENDOR_READ = "vendor:read:*";

    private final VendorBalanceHistoryRepository vendorBalanceHistoryRepository;

    @Override
    public boolean canRead(UUID fileId, CustomUserDetails user) {
        return user != null
            && user.getAuthorities().stream().anyMatch(a -> VENDOR_READ.equals(a.getAuthority()))
            && vendorBalanceHistoryRepository.existsByReceiptFileIdAndIsDeletedFalse(fileId);
    }
}
