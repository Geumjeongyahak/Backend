package geumjeongyahak.domain.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProxyService {
    private static final Set<RoleType> TEACHER_ROLES = Set.of(
        RoleType.ADMIN,
        RoleType.MANAGER,
        RoleType.VOLUNTEER
    );

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsByIdAndIsDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmailIncludingDeleted(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByDepartmentId(Long departmentId) {
        return userRepository.existsByDepartmentIdAndIsDeletedFalse(departmentId);
    }

    @Transactional(readOnly = true)
    public boolean existsByClassroomId(Long classroomId) {
        return userRepository.existsByClassroomIdAndIsDeletedFalse(classroomId);
    }
    

    @Transactional(readOnly = true)
    public boolean existsByIdAndDepartmentId(Long userId, Long departmentId) {
        return userRepository.existsByIdAndDepartmentIdAndIsDeletedFalse(userId, departmentId);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> {
                log.info("활성 사용자 조회 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
                return new UserNotFoundException(userId);
            });
    }

    @Transactional(readOnly = true)
    public Optional<User> findByIdIncludingDeleted(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public User getByIdIncludingDeleted(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> {
                log.info("사용자 이력 조회 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
                return new UserNotFoundException(userId);
            });
    }

    @Transactional(readOnly = true)
    public List<User> getAllByDepartmentId(Long departmentId) {
        return userRepository.findAllByDepartmentIdAndIsDeletedFalse(departmentId);
    }

    @Transactional(readOnly = true)
    public List<User> getTeacherCandidatesOrderByName() {
        return userRepository.findAllByIsDeletedFalse(Sort.by(Sort.Direction.ASC, "name"))
            .stream()
            .filter(user -> TEACHER_ROLES.contains(user.getRole()))
            .toList();
    }

    @Transactional(readOnly = true)
    public long countActiveUsersByDepartmentId(Long departmentId) {
        return userRepository.countByDepartmentIdAndIsDeletedFalse(departmentId);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateProfileImageUrl(Long userId, String profileImageUrl) {
        User user = getById(userId);
        user.setProfileImageUrl(profileImageUrl);
        log.info("사용자 프로필 이미지 URL 갱신 완료 - userId={}", userId);
    }
}
