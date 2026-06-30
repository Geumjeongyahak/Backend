package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPermissionService {

    private final UserProxyService userProxyService;
    private final UserPermissionRepository userPermissionRepository;

    public List<PermissionResponse> getAllPermissions(Long userId) {
        return userPermissionRepository.findAllByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public List<PermissionResponse> addPermission(Long userId, String permissionCode) {
        log.debug("사용자 권한 추가 요청 - UserID: {}, PermissionCode: {}", userId, permissionCode);
        String validatedPermissionCode = new PermissionCode(permissionCode).value();
        
        User user = userProxyService.getById(userId);
        userPermissionRepository.findByUserIdAndPermissionCode(userId, validatedPermissionCode)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(user, validatedPermissionCode)));
        
        log.info("사용자 권한 추가 완료 - UserID: {}, PermissionCode: {}", userId, validatedPermissionCode);
        return getAllPermissions(userId);
    }

    @Transactional
    public List<PermissionResponse> removePermission(Long userId, String permissionCode) {
        log.debug("사용자 권한 제거 요청 - UserID: {}, PermissionCode: {}", userId, permissionCode);
        
        userPermissionRepository.findByUserIdAndPermissionCode(userId, permissionCode)
            .ifPresent(userPermissionRepository::delete);
        
        log.info("사용자 권한 제거 완료 - UserID: {}, PermissionCode: {}", userId, permissionCode);
        return getAllPermissions(userId);
    }

    @Transactional
    public void removeAllPermissions(Long userId) {
        log.debug("사용자 권한 전체 제거 요청 - UserID: {}", userId);
        userPermissionRepository.deleteAllByUserId(userId);
        log.info("사용자 권한 전체 제거 완료 - UserID: {}", userId);
    }

    private PermissionResponse toResponse(UserPermission permission) {
        return PermissionResponse.from(
            permission.getId(),
            permission.toAuthorityCode(),
            PermissionResponse.SOURCE_MANUAL
        );
    }
}
