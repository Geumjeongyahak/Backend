package geumjeongyahak.domain.notification.repository;

import geumjeongyahak.domain.notification.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByToken(String token);

    Optional<PushSubscription> findByIdAndUserId(Long id, Long userId);

    List<PushSubscription> findAllByUserIdAndActiveTrue(Long userId);
}
