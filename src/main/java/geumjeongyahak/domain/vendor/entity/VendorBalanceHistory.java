package geumjeongyahak.domain.vendor.entity;

import java.time.LocalDateTime;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.vendor.enums.VendorBalanceHistoryType;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "vendor_balance_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VendorBalanceHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorBalanceHistoryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id")
    private File receiptFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id")
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private boolean isDeleted;

    private LocalDateTime deletedAt;

    public VendorBalanceHistory(
        Vendor vendor,
        VendorBalanceHistoryType type,
        Long amount,
        Long balanceAfter,
        String memo,
        File receiptFile,
        PurchaseRequest purchaseRequest,
        User createdBy
    ) {
        this.vendor = vendor;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.memo = memo;
        this.receiptFile = receiptFile;
        this.purchaseRequest = purchaseRequest;
        this.createdBy = createdBy;
        this.occurredAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
