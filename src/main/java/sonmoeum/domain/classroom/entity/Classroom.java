package sonmoeum.domain.classroom.entity;

import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.classroom.enums.ClassroomType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "classrooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassroomType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Classroom(String name, ClassroomType type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public void update(String name, ClassroomType type, String description) {
        if (name != null) {
            this.name = name;
        }
        if (type != null) {
            this.type = type;
        }
        if (description != null) {
            this.description = description;
        }
    }
}

