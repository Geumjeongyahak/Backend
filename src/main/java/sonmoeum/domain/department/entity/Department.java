package sonmoeum.domain.department.entity;

import lombok.*;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "assigned_role_id",nullable = false)
    private Long roleId;

    @Column(columnDefinition = "TEXT")
    private String description;


    @Builder
    public Department(
        @NonNull String name,
        @NonNull RoleType assignedRole,
        @NonNull String description
    ) {
        this.name = name;
        this.roleId = assignedRole.getId();
        this.description = description;
    }
}
