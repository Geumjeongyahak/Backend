package geumjeongyahak.domain.purchase_request.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.users.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "purchase_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PurchasePaymentType paymentType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseRequestStatus status;

    private LocalDateTime approvalAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_by")
    private User approvalBy;

    private LocalDateTime purchasedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestPaymentTransaction> transactions = new ArrayList<>();

    public PurchaseRequest(
        Classroom classroom,
        Department department,
        User requestedBy,
        PurchasePaymentType paymentType,
        String title,
        String content,
        List<PurchaseRequestItem> items
    ) {
        this.classroom = classroom;
        this.department = department;
        this.requestedBy = requestedBy;
        this.paymentType = paymentType;
        this.title = title;
        this.content = content;
        this.totalPrice = 0L;
        this.status = PurchaseRequestStatus.PENDING;
        this.isDeleted = false;
        items.forEach(item -> item.assignRequest(this));
        this.items.addAll(items);
    }

    public void approve(User approver, String note) {
        this.status = PurchaseRequestStatus.APPROVED;
        this.approvalBy = approver;
        this.approvalAt = LocalDateTime.now();
        this.note = note;
    }

    public void reportPurchase() {
        this.status = PurchaseRequestStatus.PURCHASED;
        this.purchasedAt = LocalDateTime.now();
        this.totalPrice = calculateTotalPrice();
    }

    public void confirm() {
        this.status = PurchaseRequestStatus.CONFIRMED;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void reject(User approver, String note) {
        this.status = PurchaseRequestStatus.REJECTED;
        this.approvalBy = approver;
        this.approvalAt = LocalDateTime.now();
        this.note = note;
    }

    public void update(
        String title,
        String content,
        List<PurchaseRequestItem> items
    ) {
        this.title = title;
        this.content = content;
        this.items.clear();
        items.forEach(item -> item.assignRequest(this));
        this.items.addAll(items);
    }

    public void replaceTransactions(List<PurchaseRequestPaymentTransaction> transactions) {
        this.transactions.clear();
        transactions.forEach(transaction -> transaction.assignRequest(this));
        this.transactions.addAll(transactions);
        this.totalPrice = calculateTotalPrice();
    }

    private long calculateTotalPrice() {
        return transactions.stream()
            .mapToLong(PurchaseRequestPaymentTransaction::getAmount)
            .sum();
    }
}
