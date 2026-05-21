package geumjeongyahak.domain.purchase_request.entity;

import java.util.ArrayList;
import java.util.List;

import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.vendor.entity.Vendor;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "purchase_request_payment_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private Long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id")
    private File receiptFile;

    @ElementCollection
    @CollectionTable(
        name = "purchase_request_payment_transaction_item_names",
        joinColumns = @JoinColumn(name = "transaction_id")
    )
    @OrderColumn(name = "sort_order")
    @Column(name = "item_name", nullable = false)
    private List<String> itemNames = new ArrayList<>();

    public PurchaseRequestPaymentTransaction(Vendor vendor, List<String> itemNames, Long amount, File receiptFile) {
        this.vendor = vendor;
        this.itemNames.addAll(itemNames);
        this.amount = amount;
        this.receiptFile = receiptFile;
    }

    void assignRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }

    public void clearReceiptFile() {
        this.receiptFile = null;
    }
}
