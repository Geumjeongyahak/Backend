package geumjeongyahak.domain.auth.repository;

import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    List<UserCredential> findAllByUserId(Long userId);
    
    Optional<UserCredential> findByUserIdAndProvider(Long userId, ProviderType provider);
    
    Optional<UserCredential> findByCredentialEmailAndProvider(String credentialEmail, ProviderType provider);
    
    boolean existsByCredentialEmail(String credentialEmail);

    boolean existsByUserIdAndProvider(Long userId, ProviderType provider);
    
    boolean existsByCredentialEmailAndProvider(String credentialEmail, ProviderType provider);

    Optional<UserCredential> findByProviderUserIdAndProvider(String providerUserId, ProviderType provider);

    boolean existsByProviderUserIdAndProvider(String providerUserId, ProviderType provider);

    void deleteAllByUserId(Long userId);
}
