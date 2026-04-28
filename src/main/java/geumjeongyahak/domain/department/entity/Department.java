package geumjeongyahak.domain.department.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseEntity {

    @Setter
    @Column(nullable = false, length = 100)
    private String name;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "department", fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<DepartmentPermission> permissions = new HashSet<>();

    public void addPermission(String permissionCode) {
        this.permissions.add(new DepartmentPermission(this, permissionCode));
    }

    public void clearPermissions() {
        this.permissions.clear();
    }

    @Builder
    public Department(
        @NonNull String name,
        @NonNull String description
    ) {
        this.name = name;
        this.description = description;
    }
}
