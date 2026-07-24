package geumjeongyahak.unit.purchase_request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.users.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
