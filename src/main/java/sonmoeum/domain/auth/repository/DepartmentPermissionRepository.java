package sonmoeum.domain.auth.repository;

import sonmoeum.domain.auth.entity.DepartmentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentPermissionRepository extends JpaRepository<DepartmentPermission, Long> {
}
