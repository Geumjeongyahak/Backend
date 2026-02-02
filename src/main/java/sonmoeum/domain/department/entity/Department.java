package sonmoeum.domain.department.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import sonmoeum.domain.auth.entity.DepartmentPermission;
import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.base.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(
        mappedBy = "department", fetch = FetchType.EAGER,
        cascade = CascadeType.ALL, orphanRemoval = true
    )
    private Set<DepartmentPermission> permissions = new HashSet<>();

    public Department(
        String name, 
        Collection<PermissionType> permissionTypes,
        String description
    ) {
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
        if (permissionTypes != null) {
            permissionTypes.forEach(this::addPermission);
        }
    }

    public void addPermission(PermissionType permissionType) {
        this.permissions.add(new DepartmentPermission(this, permissionType));
    }

    public void removePermission(PermissionType permissionType) {
        this.permissions.remove(new DepartmentPermission(this, permissionType));
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updatePermissions(Collection<PermissionType> newPermissionTypes) {
        this.permissions.clear();
        if (newPermissionTypes != null) {
            newPermissionTypes.forEach(this::addPermission);
        }
    }
}
