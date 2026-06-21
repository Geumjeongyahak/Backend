package geumjeongyahak.unit.purchase_request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestProxyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestProxyServiceTest {

    private static final Long USER_ID = 10L;
    private static final List<PurchaseRequestStatus> ACTIVE_STATUSES = List.of(
        PurchaseRequestStatus.PENDING,
        PurchaseRequestStatus.APPROVED
    );

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @InjectMocks
    private PurchaseRequestProxyService purchaseRequestProxyService;

    @Test
    void existsActiveByRequesterId_checksPendingAndApprovedRequests() {
        given(purchaseRequestRepository.existsByRequestedBy_IdAndStatusIn(
            USER_ID,
            ACTIVE_STATUSES
        )).willReturn(true);

        boolean exists = purchaseRequestProxyService.existsActiveByRequesterId(USER_ID);

        assertThat(exists).isTrue();
        verify(purchaseRequestRepository).existsByRequestedBy_IdAndStatusIn(
            USER_ID,
            ACTIVE_STATUSES
        );
    }
}
