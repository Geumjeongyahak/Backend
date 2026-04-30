package geumjeongyahak.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import geumjeongyahak.domain.auth.entity.RefreshToken;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    void deleteByCredentialId(Long credentialId);

    void deleteByCredentialIdIn(Iterable<Long> credentialIds);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    boolean existsByCredentialId(Long credentialId);
}
