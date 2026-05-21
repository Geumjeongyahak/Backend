package geumjeongyahak.domain.purchase_request.entity;

import geumjeongyahak.domain.file.entity.File;
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
@Table(name = "purchase_request_receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    public PurchaseRequestReceipt(File file) {
        this.file = file;
    }

    void assignRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }
}
