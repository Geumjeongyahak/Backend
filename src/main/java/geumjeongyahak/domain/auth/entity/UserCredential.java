package geumjeongyahak.domain.auth.entity;

import java.time.LocalDateTime;

import org.springframework.lang.NonNull;

import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "user_credentials",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_credentials_provider_email", columnNames = {"provider", "credential_email"}),
        @UniqueConstraint(name = "uq_user_credentials_provider_user_id", columnNames = {"provider", "provider_user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCredential extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_credentials_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "provider_user_id", length = 150)
    private String providerUserId;

    @Column(name = "credential_email", length = 100)
    @Setter
    private String credentialEmail;

    @Column(name = "email_verified", nullable = false)
    @Setter
    private boolean emailVerified;

    @Column(name = "password_hash", length = 512)
    @Setter
    private String passwordHash;

    @Column(name = "last_login_at")
    @Setter
    private LocalDateTime lastLoginAt;

    @Builder
    private UserCredential(
        User user,
        ProviderType provider,
        String providerUserId,
        String credentialEmail,
        boolean emailVerified,
        String passwordHash,
        LocalDateTime lastLoginAt
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.credentialEmail = credentialEmail;
        this.emailVerified = emailVerified;
        this.passwordHash = passwordHash;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserCredential local(
        @NonNull User user,
        @NonNull String credentialEmail,
        @NonNull String passwordHash,
        boolean emailVerified
    ) {
        return UserCredential.builder()
            .user(user)
            .provider(ProviderType.LOCAL)
            .credentialEmail(credentialEmail)
            .passwordHash(passwordHash)
            .emailVerified(emailVerified)
            .build();
    }

    public static UserCredential google(
        @NonNull User user,
        @NonNull String providerUserId,
        @NonNull String credentialEmail,
        boolean emailVerified
    ) {
        return UserCredential.builder()
            .user(user)
            .provider(ProviderType.GOOGLE)
            .providerUserId(providerUserId)
            .credentialEmail(credentialEmail)
            .emailVerified(emailVerified)
            .build();
    }
}
