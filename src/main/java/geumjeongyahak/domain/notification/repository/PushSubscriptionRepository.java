package geumjeongyahak.domain.notification.repository;

import geumjeongyahak.domain.notification.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByToken(String token);

    Optional<PushSubscription> findByIdAndUserId(Long id, Long userId);

    List<PushSubscription> findAllByUserIdAndActiveTrue(Long userId);

    @Modifying
    @Query("""
        update PushSubscription subscription
        set subscription.active = false,
            subscription.unsubscribedAt = :unsubscribedAt
        where subscription.user.id = :userId
            and subscription.active = true
        """)
    int deactivateAllByUserId(
        @Param("userId") Long userId,
        @Param("unsubscribedAt") LocalDateTime unsubscribedAt
    );
}
