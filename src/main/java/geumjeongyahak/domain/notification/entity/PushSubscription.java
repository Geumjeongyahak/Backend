package geumjeongyahak.domain.notification.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.notification.enums.PushDeviceType;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "push_subscriptions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_push_subscriptions_token", columnNames = "token")
    },
    indexes = {
        @Index(name = "idx_push_subscriptions_user_id", columnList = "user_id"),
        @Index(name = "idx_push_subscriptions_user_active", columnList = "user_id, is_active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1024)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private PushDeviceType deviceType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    public PushSubscription(
        @NonNull User user,
        @NonNull String token,
        @NonNull PushDeviceType deviceType
    ) {
        this.user = user;
        this.token = token;
        this.deviceType = deviceType;
        this.subscribedAt = LocalDateTime.now();
    }

    public void resubscribe(User user, PushDeviceType deviceType) {
        this.user = user;
        this.deviceType = deviceType;
        this.active = true;
        this.subscribedAt = LocalDateTime.now();
        this.unsubscribedAt = null;
        this.failureCount = 0;
    }

    public void unsubscribe() {
        this.active = false;
        this.unsubscribedAt = LocalDateTime.now();
    }

    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
        this.failureCount = 0;
    }

    public void markFailure() {
        this.failureCount++;
    }
}
