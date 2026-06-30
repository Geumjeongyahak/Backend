package geumjeongyahak.domain.department.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(
    name = "department_permissions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_department_permission",
            columnNames = {"department_id", "role_type", "permission_code"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    private DepartmentRoleType roleType;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    public DepartmentPermission(
        Department department, DepartmentRoleType roleType, String permissionCode
    ) {
        this.department = department;
        this.roleType = roleType;
        this.permissionCode = permissionCode;
    }

    public String toAuthorityCode() {
        return permissionCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DepartmentPermission that = (DepartmentPermission) obj;
        Long thisDepartmentId = department != null ? department.getId() : null;
        Long thatDepartmentId = that.department != null ? that.department.getId() : null;
        return Objects.equals(thisDepartmentId, thatDepartmentId)
            && roleType == that.roleType
            && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(department != null ? department.getId() : null, roleType, permissionCode);
    }
}
