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

    @Column(name = "password_reset_token_hash", length = 512)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;

    @Column(name = "password_reset_requested_at")
    private LocalDateTime passwordResetRequestedAt;

    @Column(name = "password_reset_failed_attempts", nullable = false)
    private int passwordResetFailedAttempts;

    @Column(name = "email_verification_token_hash", length = 512)
    private String emailVerificationTokenHash;

    @Column(name = "email_verification_token_expires_at")
    private LocalDateTime emailVerificationTokenExpiresAt;

    @Column(name = "email_verification_requested_at")
    private LocalDateTime emailVerificationRequestedAt;

    @Column(name = "email_verification_failed_attempts", nullable = false)
    private int emailVerificationFailedAttempts;

    @Builder
    private UserCredential(
        User user,
        ProviderType provider,
        String providerUserId,
        String credentialEmail,
        boolean emailVerified,
        String passwordHash,
        LocalDateTime lastLoginAt,
        String passwordResetTokenHash,
        LocalDateTime passwordResetTokenExpiresAt,
        LocalDateTime passwordResetRequestedAt,
        int passwordResetFailedAttempts,
        String emailVerificationTokenHash,
        LocalDateTime emailVerificationTokenExpiresAt,
        LocalDateTime emailVerificationRequestedAt,
        int emailVerificationFailedAttempts
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.credentialEmail = credentialEmail;
        this.emailVerified = emailVerified;
        this.passwordHash = passwordHash;
        this.lastLoginAt = lastLoginAt;
        this.passwordResetTokenHash = passwordResetTokenHash;
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
        this.passwordResetRequestedAt = passwordResetRequestedAt;
        this.passwordResetFailedAttempts = passwordResetFailedAttempts;
        this.emailVerificationTokenHash = emailVerificationTokenHash;
        this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt;
        this.emailVerificationRequestedAt = emailVerificationRequestedAt;
        this.emailVerificationFailedAttempts = emailVerificationFailedAttempts;
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

    public void issuePasswordResetToken(String tokenHash, LocalDateTime expiresAt) {
        issuePasswordResetToken(tokenHash, expiresAt, LocalDateTime.now());
    }

    public void issuePasswordResetToken(String tokenHash, LocalDateTime expiresAt, LocalDateTime requestedAt) {
        this.passwordResetTokenHash = tokenHash;
        this.passwordResetTokenExpiresAt = expiresAt;
        this.passwordResetRequestedAt = requestedAt;
        this.passwordResetFailedAttempts = 0;
    }

    public void issueEmailVerificationToken(
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime requestedAt
    ) {
        this.emailVerificationTokenHash = tokenHash;
        this.emailVerificationTokenExpiresAt = expiresAt;
        this.emailVerificationRequestedAt = requestedAt;
        this.emailVerificationFailedAttempts = 0;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        clearEmailVerificationToken();
    }

    public void changeCredentialEmail(String newEmail) {
        this.credentialEmail = newEmail;
        this.emailVerified = false;
        clearPasswordResetToken();
        clearEmailVerificationToken();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        clearPasswordResetToken();
    }

    public boolean isPasswordResetTokenExpired(LocalDateTime now) {
        return passwordResetTokenExpiresAt == null || !passwordResetTokenExpiresAt.isAfter(now);
    }

    public boolean isEmailVerificationTokenExpired(LocalDateTime now) {
        return emailVerificationTokenExpiresAt == null || !emailVerificationTokenExpiresAt.isAfter(now);
    }

    public void completePasswordReset(String newPasswordHash) {
        changePassword(newPasswordHash);
    }

    public void clearPasswordResetToken() {
        this.passwordResetTokenHash = null;
        this.passwordResetTokenExpiresAt = null;
        this.passwordResetRequestedAt = null;
        this.passwordResetFailedAttempts = 0;
    }

    public void clearEmailVerificationToken() {
        this.emailVerificationTokenHash = null;
        this.emailVerificationTokenExpiresAt = null;
        this.emailVerificationRequestedAt = null;
        this.emailVerificationFailedAttempts = 0;
    }

    public int recordPasswordResetFailure(int maxAttempts) {
        this.passwordResetFailedAttempts++;
        if (this.passwordResetFailedAttempts >= maxAttempts) {
            clearPasswordResetToken();
        }
        return this.passwordResetFailedAttempts;
    }

    public int recordEmailVerificationFailure(int maxAttempts) {
        this.emailVerificationFailedAttempts++;
        if (this.emailVerificationFailedAttempts >= maxAttempts) {
            clearEmailVerificationToken();
        }
        return this.emailVerificationFailedAttempts;
    }
}
