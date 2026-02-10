package sonmoeum.domain.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sonmoeum.domain.auth.enums.RoleLevel;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.auth.v1.dto.response.RoleResponse;
import sonmoeum.domain.users.entity.UserRole;
import sonmoeum.domain.users.exception.InvalidRoleOperationException;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRoleRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {
    private final UserRoleRepository userRoleRepository;
    private final UserProxyService userProxyService;


    public List<RoleResponse> getAllRoles(Long userId) {
        log.debug("전체 사용자 역할 목록 조회 요청");
        var user = userProxyService.getReferenceById(userId);
        List<RoleResponse> roles = userRoleRepository.findAllByUser(user).stream()
                .map(userRole -> RoleResponse.from(userRole.getRoleType()))
                .toList();
        log.debug("전체 사용자 역할 목록 조회 완료 - 총 {}개", roles.size());
        return roles;
    }

    public List<RoleResponse> addRole(Long userId, RoleType roleType) {
        log.info("사용자 역할 추가 요청 - UserID: {}, RoleType: {}", userId, roleType);
        if (roleType.getLevel() == RoleLevel.BASIC) {
            log.warn("기본 역할은 직접 부여할 수 없습니다 - RoleType: {}", roleType);
            throw InvalidRoleOperationException.cannotAssignBaseRole(roleType.name());
        }

        if (!userProxyService.existsById(userId)) {
            log.warn("사용자를 찾을 수 없습니다 - ID: {}", userId);
            throw new UserNotFoundException(userId);
        }
        var user = userProxyService.getReferenceById(userId);
        if (userRoleRepository.existsByUserAndRoleId(user, roleType.getId())) {
            log.warn("사용자에게 이미 해당 역할이 부여되어 있습니다 - UserID: {}, RoleType: {}", userId, roleType);
            throw InvalidRoleOperationException.roleAlreadyAssigned(userId, roleType.name());
        }
        userRoleRepository.save(new UserRole(user, roleType));
        log.info("사용자 역할 추가 완료 - UserID: {}, RoleType: {}", userId, roleType);
        return getAllRoles(userId);
    }

    public List<RoleResponse> removeRole(Long userId, RoleType roleType) {
        log.info("사용자 역할 제거 요청 - UserID: {}, RoleType: {}", userId, roleType);
        if (roleType.getLevel() == RoleLevel.BASIC) {
            log.warn("기본 역할은 직접 제거할 수 없습니다 - RoleType: {}", roleType);
            throw InvalidRoleOperationException.cannotRemoveBaseRole(roleType.name());
        }

        if (!userProxyService.existsById(userId)) {
            log.warn("사용자를 찾을 수 없습니다 - ID: {}", userId);
            throw new UserNotFoundException(userId);
        }
        var user = userProxyService.getReferenceById(userId);
        if (!userRoleRepository.existsByUserAndRoleId(user, roleType.getId())) {
            log.warn("사용자에게 해당 역할이 부여되어 있지 않습니다 - UserID: {}, RoleType: {}", userId, roleType);
            throw InvalidRoleOperationException.roleNotAssigned(userId, roleType.name());
        }
        userRoleRepository.deleteByUserAndRoleId(user, roleType.getId());
        log.info("사용자 역할 제거 완료 - UserID: {}, RoleType: {}", userId, roleType);

        return getAllRoles(userId);
    }
}
