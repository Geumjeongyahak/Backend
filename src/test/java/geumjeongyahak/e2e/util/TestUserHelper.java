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

    public User createTestUser(String nickname, String name, String email, String password, RoleType role) {
        if (userCache.containsKey(nickname)) {
            return userCache.get(nickname);
        }
        
        User user = User.builder()
                .nickname(nickname)
                .name(name)
                .email(email)
                .role(role)
                .build();
        User savedUser = userRepository.save(user);
        
        userCredentialService.createLocalCredential(savedUser, email, password);
        
        userCache.put(nickname, savedUser);
        return savedUser;
    }

    public User createTestUser(String nickname, RoleType role) {
        return createTestUser(nickname, nickname, nickname + "@test.com", getDefaultPassword(nickname), role);
    }

    public User createTestUser(String nickname, Collection<RoleType> roles) {
        RoleType role = (roles != null && !roles.isEmpty()) ? roles.iterator().next() : RoleType.VOLUNTEER;
        return createTestUser(nickname, role);
    }

    public void setUser(String nickname) {
        userRepository.findByNickname(nickname)
            .ifPresent(user -> userCache.put(nickname, user));
    }

    public User getUser(String nickname) {
        User user = userCache.get(nickname);
        if (user == null) {
            userRepository.findByNickname(nickname)
                .ifPresent(u -> userCache.put(nickname, u));
            user = userCache.get(nickname);
        }
        return user;
    }

    public String getDefaultPassword(String nickname) {
        if ("admin1234".equals(nickname)) return "admin1234";
        if ("teacher01".equals(nickname)) return "teacher01";
        if ("teacher02".equals(nickname)) return "teacher02";
        return "pw_" + nickname;
    }

    public String generateAccessToken(Long userId) {
        return jwtTokenProvider.createAccessToken(String.valueOf(userId));
    }

    public String generateToken(Long userId, Long expSeconds) {
        return jwtTokenProvider.createToken(String.valueOf(userId), expSeconds);
    }

    public String generateAccessTokenByNickname(String nickname) {
        User user = getUser(nickname);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + nickname);
        }
        return generateAccessToken(user.getId());
    }

    public String generateAccessTokenByNickname(String nickname, Long expSeconds) {
        User user = getUser(nickname);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + nickname);
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
