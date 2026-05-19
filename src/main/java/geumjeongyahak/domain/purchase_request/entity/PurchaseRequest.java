package geumjeongyahak.domain.purchase_request.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentMethod;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.vendor.entity.Vendor;

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
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "total_price")
    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PurchasePaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

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

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestReceipt> receipts = new ArrayList<>();

    public PurchaseRequest(
        Classroom classroom,
        User requestedBy,
        String title,
        String content,
        PurchasePaymentMethod paymentMethod,
        Vendor vendor,
        List<PurchaseRequestItem> items,
        List<PurchaseRequestReceipt> receipts
    ) {
        this.classroom = classroom;
        this.requestedBy = requestedBy;
        this.title = title;
        this.content = content;
        this.paymentMethod = paymentMethod;
        this.vendor = vendor;
        this.totalPrice = calculateTotalPrice(items);
        this.status = PurchaseRequestStatus.PENDING;
        items.forEach(item -> item.assignRequest(this));
        this.items.addAll(items);
        receipts.forEach(receipt -> receipt.assignRequest(this));
        this.receipts.addAll(receipts);
    }

    public void approve(User approver, String note) {
        this.status = PurchaseRequestStatus.APPROVED;
        this.approvalBy = approver;
        this.approvalAt = LocalDateTime.now();
        this.note = note;
    }

    public void reportPurchase(List<PurchaseRequestReceipt> receipts) {
        this.status = PurchaseRequestStatus.PURCHASED;
        this.purchasedAt = LocalDateTime.now();
        this.receipts.clear();
        receipts.forEach(receipt -> receipt.assignRequest(this));
        this.receipts.addAll(receipts);
    }

    public void confirm() {
        this.status = PurchaseRequestStatus.CONFIRMED;
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
        PurchasePaymentMethod paymentMethod,
        Vendor vendor,
        List<PurchaseRequestItem> items
    ) {
        this.title = title;
        this.content = content;
        this.paymentMethod = paymentMethod;
        this.vendor = vendor;
        this.totalPrice = calculateTotalPrice(items);
        this.items.clear();
        items.forEach(item -> item.assignRequest(this));
        this.items.addAll(items);
    }

    private long calculateTotalPrice(List<PurchaseRequestItem> items) {
        return items.stream()
            .mapToLong(PurchaseRequestItem::getPrice)
            .sum();
    }
}
