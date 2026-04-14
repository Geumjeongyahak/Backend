package geumjeongyahak.domain.classroom.entity;

import lombok.*;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.classroom.enums.ClassroomType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Getter
@Table(name = "classrooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom extends BaseEntity {

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassroomType type;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder
    public Classroom(
            @NonNull String name,
            @NonNull ClassroomType type,
            String description
    ) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.isDeleted = false;
    }
}

