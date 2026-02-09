package sonmoeum.domain.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.entity.UserRole;

import java.util.Arrays;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findAllByUser(User user);
    boolean existsByUserAndRoleId(User user, Long roleId);

    void deleteByUserAndRoleId(User user, Long roleId);
}
