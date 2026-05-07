package geumjeongyahak.domain.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.student.enums.StudentStatus;

@Entity
@Getter
@Table(name = "students")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ENROLLED;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public Student(String name, String phoneNumber, String description, Classroom classroom) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.description = description;
        this.classroom = classroom;
        this.status = StudentStatus.ENROLLED;
        this.isDeleted = false;
    }

    public void update(String name, String phoneNumber, String description, StudentStatus status, Classroom classroom) {
        if (name != null) {
            this.name = name;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (description != null) {
            this.description = description;
        }
        if (status != null) {
            this.status = status;
        }
        if (classroom != null) {
            this.classroom = classroom;
        }
    }

    public void delete() {
        this.isDeleted = true;
    }
}
