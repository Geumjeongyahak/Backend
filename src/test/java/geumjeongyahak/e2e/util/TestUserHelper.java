package geumjeongyahak.e2e.util;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final Map<String, User> userCache;

    public TestUserHelper(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            UserCredentialService userCredentialService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userCredentialService = userCredentialService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userCache = new HashMap<>();
    }

    public User createTestUser(String userKey, String name, String email, String password, RoleType role) {
        if (userCache.containsKey(userKey)) {
            return userCache.get(userKey);
        }
        
        User user = User.builder()
                .name(name)
                .email(email)
                .role(role)
                .build();
        User savedUser = userRepository.save(user);
        
        userCredentialService.createLocalCredential(savedUser, email, password);
        
        userCache.put(userKey, savedUser);
        return savedUser;
    }

    public User createTestUser(String userKey, RoleType role) {
        return createTestUser(userKey, userKey, userKey + "@test.com", getDefaultPassword(userKey), role);
    }

    public User createTestUser(String userKey, Collection<RoleType> roles) {
        RoleType role = (roles != null && !roles.isEmpty()) ? roles.iterator().next() : RoleType.VOLUNTEER;
        return createTestUser(userKey, role);
    }

    public void setUser(String userKey) {
        userRepository.findByEmail(toDefaultEmail(userKey))
            .ifPresent(user -> userCache.put(userKey, user));
    }

    public User getUser(String userKey) {
        User user = userCache.get(userKey);
        if (user == null) {
            userRepository.findByEmail(toDefaultEmail(userKey))
                .ifPresent(u -> userCache.put(userKey, u));
            user = userCache.get(userKey);
        }
        return user;
    }

    public String getDefaultPassword(String userKey) {
        if ("admin1234".equals(userKey)) return "admin1234";
        if ("teacher01".equals(userKey)) return "teacher01";
        if ("teacher02".equals(userKey)) return "teacher02";
        return "pw_" + userKey;
    }

    public String generateAccessToken(Long userId) {
        return jwtTokenProvider.createAccessToken(String.valueOf(userId));
    }

    public String generateToken(Long userId, Long expSeconds) {
        return jwtTokenProvider.createToken(String.valueOf(userId), expSeconds);
    }

    public String generateAccessTokenByUserKey(String userKey) {
        User user = getUser(userKey);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userKey);
        }
        return generateAccessToken(user.getId());
    }

    public String generateAccessTokenByUserKey(String userKey, Long expSeconds) {
        User user = getUser(userKey);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userKey);
        }
        return generateToken(user.getId(), expSeconds);
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
