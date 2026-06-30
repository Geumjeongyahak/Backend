package geumjeongyahak.domain.department.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    @Builder
    public Department(
        @NonNull String name,
        @NonNull String description
    ) {
        this.name = name;
        this.description = description;
    }
}
