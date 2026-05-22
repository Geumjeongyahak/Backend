package geumjeongyahak.e2e.util;

import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
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
        if (userCache.containsKey(email)) {
            return userCache.get(email);
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .name(name)
                    .email(email)
                    .role(role)
                    .build();
            return userRepository.save(newUser);
        });
        
        userCredentialRepository.findByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .ifPresentOrElse(
                credential -> userCredentialService.updateLocalPassword(user, passwordEncoder.encode(password)),
                () -> userCredentialService.createLocalCredential(user, email, password)
            );
        
        userCache.put(email, user);
        return user;
    }

    public User createTestUser(String email, RoleType role) {
        String name = email.split("@")[0];
        return createTestUser(email, name, getDefaultPassword(email), role);
    }

    public User createTestUser(String email, Collection<RoleType> roles) {
        RoleType role = (roles != null && !roles.isEmpty()) ? roles.iterator().next() : RoleType.VOLUNTEER;
        return createTestUser(email, role);
    }

    public User setTeacherPeriod(String email, LocalDate teacherStartAt, LocalDate teacherEndAt) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        user.setTeacherStartAt(teacherStartAt);
        user.setTeacherEndAt(teacherEndAt);
        userCache.put(email, user);
        return user;
    }

    public void setUser(String email) {
        userRepository.findByEmail(email)
            .ifPresent(user -> userCache.put(email, user));
    }

    public User getUser(String email) {
        User user = userCache.get(email);
        if (user == null) {
            userRepository.findByEmail(email)
                .ifPresent(u -> userCache.put(email, u));
            user = userCache.get(email);
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
