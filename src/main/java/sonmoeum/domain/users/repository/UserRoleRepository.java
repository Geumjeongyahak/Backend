package sonmoeum.domain.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.entity.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findAllByUser(User user);
    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);
}
