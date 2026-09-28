package geumjeongyahak.unit.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.vendor.entity.Vendor;
import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import geumjeongyahak.domain.vendor.repository.VendorRepository;
import geumjeongyahak.domain.vendor.service.VendorService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VendorServiceDeductOrderTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorBalanceHistoryRepository vendorBalanceHistoryRepository;

    @InjectMocks
    private VendorService vendorService;

    @Test
    @DisplayName("여러 거래처를 차감할 때 거래처 ID 오름차순으로 잠근다")
    void deductForPurchaseRequest_locksVendorsInIdOrder() {
        Map<Long, Vendor> vendorsById = new HashMap<>();
        Map<Vendor, Long> amountByVendor = new HashMap<>();
        for (long id : List.of(5L, 3L, 9L, 1L, 7L)) {
            Vendor vendor = vendorWithBalance(id, 1000L);
            vendorsById.put(id, vendor);
            amountByVendor.put(vendor, 100L);
        }
        List<Long> lockedVendorIds = new ArrayList<>();
        given(vendorRepository.findByIdForUpdate(anyLong())).willAnswer(invocation -> {
            Long vendorId = invocation.getArgument(0);
            lockedVendorIds.add(vendorId);
            return Optional.of(vendorsById.get(vendorId));
        });

        vendorService.deductForPurchaseRequest(amountByVendor, mock(PurchaseRequest.class), mock(User.class));

        assertThat(lockedVendorIds).containsExactly(1L, 3L, 5L, 7L, 9L);
    }

    private Vendor vendorWithBalance(Long id, long balance) {
        Vendor vendor = new Vendor("거래처 " + id, null);
        ReflectionTestUtils.setField(vendor, "id", id);
        vendor.charge(balance);
        return vendor;
    }
}
