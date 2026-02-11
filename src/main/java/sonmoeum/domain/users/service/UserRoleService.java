package sonmoeum.domain.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.auth.v1.dto.response.RoleResponse;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.InvalidRoleOperationException;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRepository;
import sonmoeum.domain.users.repository.UserRoleRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles(Long userId) {
        log.debug("전체 사용자 역할 목록 조회 요청");
        var user = userRepository.getReferenceById(userId);
        List<RoleResponse> roles = userRoleRepository.findAllByUser(user).stream()
                .map(userRole -> RoleResponse.from(userRole.getRoleType()))
                .toList();
        log.debug("전체 사용자 역할 목록 조회 완료 - 총 {}개", roles.size());
        return roles;
    }

    @Transactional
    public List<RoleResponse> addRole(Long userId, RoleType roleType) {
        log.info("사용자 역할 추가 요청 - UserID: {}, RoleType: {}", userId, roleType);
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("사용자를 찾을 수 없습니다 - ID: {}", userId);
            return new UserNotFoundException(userId);
        });
        if (user.hasRole(roleType)) {
            log.warn("사용자에게 이미 해당 역할이 부여되어 있습니다 - UserID: {}, RoleType: {}", userId, roleType);
            throw InvalidRoleOperationException.roleAlreadyAssigned(userId, roleType.name());
        }
        user.addRole(roleType);
        log.info("사용자 역할 추가 완료 - UserID: {}, RoleType: {}", userId, roleType);
        return user.getRoles().stream()
                .map(userRole -> RoleResponse.from(userRole.getRoleType()))
                .toList();
    }

    @Transactional
    public void addRoleIfNotExist(Long userId, RoleType roleType) {
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("사용자를 찾을 수 없습니다 - ID: {}", userId);
            return new UserNotFoundException(userId);
        });
        if (!user.hasRole(roleType)) {
            user.addRole(roleType);
            log.info("사용자 역할 추가 완료(멱등성) - UserID: {}, RoleType: {}", userId, roleType);
        }
    }

    @Transactional
    public List<RoleResponse> removeRole(Long userId, RoleType roleType) {
        log.info("사용자 역할 제거 요청 - UserID: {}, RoleType: {}", userId, roleType);
        User user = userRepository.getReferenceById(userId);
        if (!user.hasRole(roleType)) {
            log.warn("사용자에게 해당 역할이 부여되어 있지 않습니다 - UserID: {}, RoleType: {}", userId, roleType);
            throw InvalidRoleOperationException.roleNotAssigned(userId, roleType.name());
        }
        user.removeRole(roleType);
        log.info("사용자 역할 제거 완료 - UserID: {}, RoleType: {}", userId, roleType);
        return user.getRoles().stream()
                .map(userRole -> RoleResponse.from(userRole.getRoleType()))
                .toList();
    }

    @Transactional
    public void removeRoleIfExist(Long userId, RoleType roleType) {
        log.info("사용자 역할 제거 요청(존재 시) - UserID: {}, Role: {}", userId, roleType);

        userRoleRepository.findByUserIdAndRoleId(userId, roleType.getId()).ifPresent(userRole -> {
            userRoleRepository.delete(userRole);
            log.info("사용자 역할 제거 완료(멱등성) - UserID: {}, RoleType: {}", userId, roleType);
        });
    }
}
