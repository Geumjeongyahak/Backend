package sonmoeum.domain.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;

import java.util.Optional;

/**
 * User 도메인의 Proxy Service
 * 다른 도메인(auth 등)에서 User 엔티티에 접근할 때 사용
 * 역할:
 * - 간단한 조회 및 존재 확인 (Repository 직접 접근)
 * - 단순 저장 (비즈니스 로직 없음)
 * 복잡한 CRUD 작업(검증, 중복 확인 포함)은 UserAdminService 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProxyService {
    private final UserRepository userRepository;

    // ============== 조회 메서드 (읽기 전용) ==============

    /**
     * 사용자 ID 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    /**
     * 사용자명 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 이메일 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 사용자 참조 조회 (Lazy Loading용)
     */
    @Transactional(readOnly = true)
    public User getReferenceById(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    /**
     * 사용자 ID로 조회
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 사용자 ID로 조회 (없으면 예외 발생)
     */
    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));
    }

    /**
     * 사용자명으로 조회
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 사용자명으로 조회 (없으면 예외 발생)
     */
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. username=" + username));
    }

    // ============== 저장 메서드 (단순 저장만, 비즈니스 로직 없음) ==============

    /**
     * 사용자 저장 (단순 저장만 수행, 검증 없음)
     * 비즈니스 로직이 포함된 생성/수정/삭제는 UserAdminService 사용
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
