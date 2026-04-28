package geumjeongyahak.domain.users.repository;

import geumjeongyahak.domain.users.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findAllByUserId(Long userId);

    Optional<UserPermission> findByUserIdAndPermissionCode(Long userId, String permissionCode);
}
