package geumjeongyahak.e2e.util;

import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestUserHelper {
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserCredentialService userCredentialService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, User> userCache;

    public TestUserHelper(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            UserCredentialService userCredentialService,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userCredentialService = userCredentialService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userCache = new HashMap<>();
    }

    public User createTestUser(String email, String name, String password, RoleType role) {
        String normalizedEmail = toDefaultEmail(email);
        if (userCache.containsKey(normalizedEmail)) {
            return userCache.get(normalizedEmail);
        }

        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User newUser = User.builder()
                .name(name)
                .email(normalizedEmail)
                .role(role)
                .build();
            return userRepository.save(newUser);
        });

        userCredentialRepository.findByCredentialEmailAndProvider(normalizedEmail, ProviderType.LOCAL)
            .ifPresentOrElse(
                credential -> userCredentialService.updateLocalPassword(user, passwordEncoder.encode(password)),
                () -> userCredentialService.createLocalCredential(user, normalizedEmail, password)
            );

        userCache.put(normalizedEmail, user);
        return user;
    }

    public User createTestUser(String userKey, String name, String email, String password, RoleType role) {
        return createTestUser(email, name, password, role);
    }

    public User createTestUser(String email, RoleType role) {
        String normalizedEmail = toDefaultEmail(email);
        String name = normalizedEmail.split("@")[0];
        return createTestUser(normalizedEmail, name, getDefaultPassword(email), role);
    }

    public User createTestUser(String email, Collection<RoleType> roles) {
        RoleType role = (roles != null && !roles.isEmpty()) ? roles.iterator().next() : RoleType.VOLUNTEER;
        return createTestUser(email, role);
    }

    public User setTeacherPeriod(String email, LocalDate teacherStartAt, LocalDate teacherEndAt) {
        String normalizedEmail = toDefaultEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + normalizedEmail));
        user.setTeacherStartAt(teacherStartAt);
        user.setTeacherEndAt(teacherEndAt);
        userCache.put(normalizedEmail, user);
        return user;
    }

    public void setUser(String email) {
        String normalizedEmail = toDefaultEmail(email);
        userRepository.findByEmail(normalizedEmail)
            .ifPresent(user -> userCache.put(normalizedEmail, user));
    }

    public User getUser(String email) {
        String normalizedEmail = toDefaultEmail(email);
        User user = userCache.get(normalizedEmail);
        if (user == null) {
            userRepository.findByEmail(normalizedEmail)
                .ifPresent(u -> userCache.put(normalizedEmail, u));
            user = userCache.get(normalizedEmail);
        }
        return user;
    }

    public String getDefaultPassword(String identifier) {
        if ("admin@test.com".equals(identifier) || "admin1234".equals(identifier)) return "admin1234";
        if ("teacher01@test.com".equals(identifier) || "teacher01".equals(identifier)) return "teacher01";
        if ("teacher02@test.com".equals(identifier) || "teacher02".equals(identifier)) return "teacher02";
        return "pw_" + identifier;
    }

    public String generateAccessToken(Long userId) {
        return jwtTokenProvider.createAccessToken(String.valueOf(userId));
    }

    public String generateToken(Long userId, Long expSeconds) {
        return jwtTokenProvider.createToken(String.valueOf(userId), expSeconds);
    }

    public String generateAccessTokenByEmail(String email) {
        User user = getUser(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + email);
        }
        return generateAccessToken(user.getId());
    }

    public String generateAccessTokenByEmail(String email, Long expSeconds) {
        User user = getUser(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + email);
        }
        return generateToken(user.getId(), expSeconds);
    }

    public String generateAccessTokenByUserKey(String userKey) {
        return generateAccessTokenByEmail(userKey);
    }

    public String generateAccessTokenByUserKey(String userKey, Long expSeconds) {
        return generateAccessTokenByEmail(userKey, expSeconds);
    }

    private String toDefaultEmail(String userKey) {
        if (userKey.contains("@")) {
            return userKey;
        }
        return switch (userKey) {
            case "admin1234" -> "admin@test.com";
            case "teacher01" -> "teacher01@test.com";
            case "teacher02" -> "teacher02@test.com";
            case "guest01" -> "guest01@test.com";
            default -> userKey + "@test.com";
        };
    }

    public void clearAll() {
        userCache.values().stream()
            .map(User::getId)
            .filter(id -> id != null && id > 4)
            .forEach(id -> {
                userCredentialRepository.deleteAllByUserId(id);
                userRepository.deleteById(id);
            });
        userCache.clear();
    }
}
