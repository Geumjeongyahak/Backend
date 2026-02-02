package sonmoeum.domain.auth.entity;

import java.util.Objects;

import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.department.entity.Department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "department_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentPermission extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    public PermissionType getPermissionType() {
        return PermissionType.findById(permissionId).orElse(null);
    }

    public DepartmentPermission(Department department, PermissionType permissionType) {
        this.department = department;
        this.permissionId = permissionType.getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DepartmentPermission that = (DepartmentPermission) obj;
        
        Long thisDepartmentId = (department != null) ? department.getId() : null;
        Long thatDepartmentId = (that.department != null) ? that.department.getId() : null;
        
        return Objects.equals(thisDepartmentId, thatDepartmentId) && 
               Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        Long departmentId = (department != null) ? department.getId() : null;
        return Objects.hash(departmentId, permissionId);
    }
}
