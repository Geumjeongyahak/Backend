package geumjeongyahak.domain.department.repository;

import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DepartmentPermissionRepository extends JpaRepository<DepartmentPermission, Long> {

    List<DepartmentPermission> findAllByDepartmentId(Long departmentId);

    List<DepartmentPermission> findAllByDepartmentIdAndRoleTypeIn(
        Long departmentId, Collection<DepartmentRoleType> roleTypes
    );

    void deleteAllByDepartmentId(Long departmentId);

    Optional<DepartmentPermission> findByDepartmentIdAndRoleTypeAndPermissionCode(
        Long departmentId, DepartmentRoleType roleType, String permissionCode
    );
}
