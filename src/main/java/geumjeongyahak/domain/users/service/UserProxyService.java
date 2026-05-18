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
        return userRepository.existsById(userId);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByDepartmentId(Long departmentId) {
        return userRepository.existsByDepartmentId(departmentId);
    }
    

    @Transactional(readOnly = true)
    public boolean existsByIdAndDepartmentId(Long userId, Long departmentId) {
        return userRepository.existsByIdAndDepartmentId(userId, departmentId);
    }

    @Transactional(readOnly = true)
    public User getReferenceById(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public java.util.List<User> getAllByDepartmentId(Long departmentId) {
        return userRepository.findAllByDepartmentId(departmentId);
    }

    @Transactional(readOnly = true)
    public List<User> getTeacherCandidatesOrderByName() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
            .stream()
            .filter(user -> TEACHER_ROLES.contains(user.getRole()))
            .toList();
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
