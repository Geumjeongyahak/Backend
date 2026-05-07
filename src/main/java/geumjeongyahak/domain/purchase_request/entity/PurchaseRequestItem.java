package geumjeongyahak.domain.purchase_request.entity;

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

    private Long expectedPrice;

    private Long actualPrice;

    public PurchaseRequestItem(String name, String reason) {
        this.name = name;
        this.reason = reason;
    }

    public PurchaseRequestItem(String name, String reason, Long expectedPrice) {
        this.name = name;
        this.reason = reason;
        this.expectedPrice = expectedPrice;
    }

    void assignRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }

    public void updatePurchaseDetails(Long price) {
        this.actualPrice = price;
    }
}
