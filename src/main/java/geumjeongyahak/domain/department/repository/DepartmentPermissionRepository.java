package geumjeongyahak.domain.department.repository;

import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.entity.DepartmentPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentPermissionRepository extends JpaRepository<DepartmentPermission, Long> {
    List<DepartmentPermission> findAllByDepartment(Department department);
    List<DepartmentPermission> findAllByDepartmentId(Long departmentId);
    Optional<DepartmentPermission> findByDepartmentIdAndPermissionCode(Long departmentId, String permissionCode);
}
