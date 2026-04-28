package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.auth.v1.dto.response.RoleResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPermissionService {

    private final UserProxyService userProxyService;
    private final UserPermissionRepository userPermissionRepository;

    public List<RoleResponse> getAllPermissions(Long userId) {
        return userPermissionRepository.findAllByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public List<RoleResponse> addPermission(Long userId, String permissionCode) {
        User user = userProxyService.getById(userId);
        userPermissionRepository.findByUserIdAndPermissionCode(userId, permissionCode)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(user, permissionCode)));
        return getAllPermissions(userId);
    }

    @Transactional
    public List<RoleResponse> removePermission(Long userId, String permissionCode) {
        userPermissionRepository.findByUserIdAndPermissionCode(userId, permissionCode)
            .ifPresent(userPermissionRepository::delete);
        return getAllPermissions(userId);
    }

    private RoleResponse toResponse(UserPermission permission) {
        return new RoleResponse(permission.toAuthorityCode(), permission.toAuthorityCode());
    }
}
