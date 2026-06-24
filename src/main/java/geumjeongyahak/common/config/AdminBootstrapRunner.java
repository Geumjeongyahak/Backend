package geumjeongyahak.common.config;

import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        String email = normalizeEmail(properties.getEmail());
        if (!StringUtils.hasText(email)) {
            if (userRepository.countByRoleAndIsDeletedFalse(RoleType.ADMIN) > 0) {
                log.warn("admin_bootstrap_skipped reason=admin_exists_without_configured_email");
                return;
            }
            throw new IllegalStateException("ADMIN_EMAIL must be set before starting production without an existing admin account.");
        }

        userCredentialRepository.findByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .ifPresentOrElse(
                credential -> ensureAdminRole(credential.getUser(), email),
                () -> ensureAdminUserWithLocalCredential(email)
            );
    }

    private void ensureAdminUserWithLocalCredential(String email) {
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(User.builder()
                .name(resolveAdminName())
                .email(email)
                .role(RoleType.ADMIN)
                .build()));

        if (user.isDeleted()) {
            throw new IllegalStateException("ADMIN_EMAIL belongs to a deleted user. Restore or purge that user before bootstrap.");
        }

        ensureAdminRole(user, email);

        if (!userCredentialRepository.existsByUserIdAndProvider(user.getId(), ProviderType.LOCAL)) {
            String password = requireStrongPassword();
            UserCredential credential = UserCredential.local(
                user,
                email,
                passwordEncoder.encode(password),
                true
            );
            userCredentialRepository.save(credential);
            log.warn("admin_bootstrap_created email={}", email);
            return;
        }

        log.warn("admin_bootstrap_verified email={}", email);
    }

    private void ensureAdminRole(User user, String email) {
        if (user.getRole() != RoleType.ADMIN) {
            user.setRole(RoleType.ADMIN);
            log.warn("admin_bootstrap_promoted userId={} email={}", user.getId(), email);
        }
        if (!StringUtils.hasText(user.getEmail())) {
            user.setEmail(email);
        }
    }

    private String requireStrongPassword() {
        String password = properties.getPassword();
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("ADMIN_PASSWORD must be set to create the initial production admin account.");
        }
        if (!isStrongPassword(password)) {
            throw new IllegalStateException(
                "ADMIN_PASSWORD must be at least 12 characters and include lowercase, uppercase, digit, and symbol characters."
            );
        }
        return password;
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }

        return hasLower && hasUpper && hasDigit && hasSymbol;
    }

    private String resolveAdminName() {
        return StringUtils.hasText(properties.getName()) ? properties.getName().trim() : "관리자";
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }
}
