package geumjeongyahak.e2e.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestUserHelper {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, User> userCache;

    public TestUserHelper(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userCache = new HashMap<>();
    }

    public User createTestUser(String name, String username, String password, Collection<RoleType> roles) {
        return userCache.computeIfAbsent(username, key -> {
            User user = User.localBuilder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .name(name)
                    .roles(roles)
                    .build();
            return userRepository.save(user);
        });
    }

    public User createTestUser(String username, Collection<RoleType> roles) {
        return createTestUser(username, username, getDefaultPassword(username), roles);
    }
    public void setUser(String username) {
        userRepository.findByUsername(username)
            .ifPresent(user -> userCache.put(username, user));
    }

    public User getUser(String username) {
        return userCache.get(username);
    }

    public String getDefaultPassword(String username) {
        return "pw_" + username;
    }

    public String generateAccessToken(String username) {
        return jwtTokenProvider.createAccessToken(username);
    }

    public String generateToken(String username, Long expSeconds) {
        return jwtTokenProvider.createToken(username, expSeconds);
    }

    public void clearAll() {
        // ID로 삭제하여 영속성 컨텍스트 문제 방지
        userCache.values().stream()
                .map(User::getId)
                .filter(id -> id != null)
                .forEach(userRepository::deleteById);
        userCache.clear();
    }

}
