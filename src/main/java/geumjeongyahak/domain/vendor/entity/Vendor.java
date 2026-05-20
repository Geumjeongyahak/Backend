package geumjeongyahak.domain.vendor.entity;

import java.time.LocalDateTime;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.vendor.exception.VendorErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "vendors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vendor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long balance;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private boolean isDeleted;

    private LocalDateTime deletedAt;

    public Vendor(String name, String description) {
        this.name = name;
        this.description = description;
        this.balance = 0L;
        this.isActive = true;
        this.isDeleted = false;
    }

    public void update(String name, String description, Boolean isActive) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void charge(long amount) {
        this.balance += amount;
    }

    public void deduct(long amount) {
        if (this.balance < amount) {
            throw new BusinessException(VendorErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance -= amount;
    }

    public void delete() {
        this.isDeleted = true;
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
    }
}
