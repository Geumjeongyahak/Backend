package sonmoeum.domain.users.service;

import sonmoeum.api.v1.auth.dto.response.PermissionListResponse;
import sonmoeum.api.v1.users.dto.request.AddUserPermissionsRequest;
import sonmoeum.domain.auth.enums.PermissionGranterType;
import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPermissionService {
    private final UserRepository userRepository;

    public PermissionListResponse getUserPermissions(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다.")
        );
        var permissions = user.getPermissions()
            .stream()
            .map(Enum::name)
            .toList();
        return new PermissionListResponse(permissions);
    }
    
    public PermissionListResponse addUserPermissions(
        Long userId,
        AddUserPermissionsRequest request
    ) {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다.")
        );
        for (var permStr : request.permissions()) {
            // Manual addition -> GranterType.USER
            user.addPermission(PermissionType.valueOf(permStr), PermissionGranterType.USER);
        }
        userRepository.save(user);
        return new PermissionListResponse(user.getPermissions()
            .stream()
            .map(Enum::name)
            .toList());
    }

    public PermissionListResponse removeUserPermissions(
        Long userId,
        AddUserPermissionsRequest request
    ) {
        var user = userRepository.findById(userId).orElseThrow(
            () -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다.")
        );
        for (var permStr : request.permissions()) {
            user.removePermission(PermissionType.valueOf(permStr), PermissionGranterType.USER);
        }
        userRepository.save(user);
        return new PermissionListResponse(user.getPermissions()
            .stream()
            .map(Enum::name)
            .toList());
    }
}
