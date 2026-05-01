package geumjeongyahak.domain.department.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
            columnNames = {"department_id", "permission_code"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    public DepartmentPermission(
        Department department, String permissionCode
    ) {
        this.department = department;
        this.permissionCode = permissionCode;
    }

    public boolean isGlobalScope() {
        return permissionCode != null && permissionCode.endsWith(":*");
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
            && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(department != null ? department.getId() : null, permissionCode);
    }
}
