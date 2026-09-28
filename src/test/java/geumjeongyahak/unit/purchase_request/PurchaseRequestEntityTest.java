package geumjeongyahak.unit.purchase_request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestPaymentTransaction;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.vendor.entity.Vendor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PurchaseRequestEntityTest {

    @Test
    void constructor_storesRequestLevelPaymentTypeAndDepartment() {
        Classroom classroom = mock(Classroom.class);
        Department department = mock(Department.class);
        User requester = mock(User.class);
        PurchaseRequestItem item = new PurchaseRequestItem("교재", "수업 준비", 2);

        PurchaseRequest purchaseRequest = new PurchaseRequest(
            classroom,
            department,
            requester,
            PurchasePaymentType.PREPAID,
            "교재 구입",
            null,
            List.of(item)
        );

        assertThat(purchaseRequest.getClassroom()).isSameAs(classroom);
        assertThat(purchaseRequest.getDepartment()).isSameAs(department);
        assertThat(purchaseRequest.getRequestedBy()).isSameAs(requester);
        assertThat(purchaseRequest.getPaymentType()).isEqualTo(PurchasePaymentType.PREPAID);
        assertThat(purchaseRequest.getContent()).isNull();
        assertThat(purchaseRequest.isDeleted()).isFalse();
        assertThat(purchaseRequest.getDeletedAt()).isNull();
        assertThat(purchaseRequest.getItems()).containsExactly(item);
        assertThat(item.getPurchaseRequest()).isSameAs(purchaseRequest);
    }

    @Test
    void softDelete_marksRequestAsDeletedAndRecordsTimestamp() {
        PurchaseRequest purchaseRequest = new PurchaseRequest(
            mock(Classroom.class),
            null,
            mock(User.class),
            PurchasePaymentType.ACTUAL,
            "비품 구입",
            "교실 비품을 구입합니다.",
            List.of(new PurchaseRequestItem("마커", "수업 준비", 1))
        );

        purchaseRequest.softDelete();

        assertThat(purchaseRequest.isDeleted()).isTrue();
        assertThat(purchaseRequest.getDeletedAt()).isNotNull();
    }

    @Test
    void sumAmountByVendorInIdOrder_ordersVendorsByIdAndMergesSameVendor() {
        PurchaseRequest purchaseRequest = new PurchaseRequest(
            mock(Classroom.class),
            null,
            mock(User.class),
            PurchasePaymentType.ACTUAL,
            "비품 구입",
            null,
            List.of(new PurchaseRequestItem("마커", "수업 준비", 1))
        );
        Vendor vendor5 = vendorWithId(5L);
        Vendor vendor3 = vendorWithId(3L);
        Vendor vendor9 = vendorWithId(9L);
        Vendor vendor1 = vendorWithId(1L);
        Vendor vendor7 = vendorWithId(7L);
        purchaseRequest.replaceTransactions(List.of(
            transaction(vendor5, 500L),
            transaction(vendor3, 300L),
            transaction(vendor9, 900L),
            transaction(vendor1, 100L),
            transaction(vendor7, 700L),
            transaction(vendor3, 30L)
        ));

        Map<Vendor, Long> amountByVendor = purchaseRequest.sumAmountByVendorInIdOrder();

        assertThat(amountByVendor).containsExactly(
            Map.entry(vendor1, 100L),
            Map.entry(vendor3, 330L),
            Map.entry(vendor5, 500L),
            Map.entry(vendor7, 700L),
            Map.entry(vendor9, 900L)
        );
    }

    private Vendor vendorWithId(Long id) {
        Vendor vendor = new Vendor("거래처 " + id, null);
        ReflectionTestUtils.setField(vendor, "id", id);
        return vendor;
    }

    private PurchaseRequestPaymentTransaction transaction(Vendor vendor, long amount) {
        return new PurchaseRequestPaymentTransaction(vendor, List.of("품목"), amount, null, null);
    }
}
