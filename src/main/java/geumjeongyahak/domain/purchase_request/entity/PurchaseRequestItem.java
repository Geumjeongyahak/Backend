package geumjeongyahak.domain.purchase_request.entity;

import geumjeongyahak.domain.file.entity.File;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "purchase_requests_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private Long price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id")
    private File receiptFile;

    public PurchaseRequestItem(String name, String reason) {
        this.name = name;
        this.reason = reason;
    }

    public PurchaseRequestItem(String name, String reason, Long price, File receiptFile) {
        this.name = name;
        this.reason = reason;
        this.price = price;
        this.receiptFile = receiptFile;
    }

    void assignRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }

    public void updateReceipt(File receiptFile) {
        this.receiptFile = receiptFile;
    }
}
