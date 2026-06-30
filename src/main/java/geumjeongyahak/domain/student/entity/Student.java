package geumjeongyahak.domain.student.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "student", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<StudentClassroom> studentClassrooms = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ENROLLED;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public Student(String name, String phoneNumber, String description, Classroom classroom) {
        this(name, phoneNumber, description, List.of(classroom));
    }

    public Student(String name, String phoneNumber, String description, Collection<Classroom> classrooms) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.description = description;
        setClassrooms(classrooms);
        this.status = StudentStatus.ENROLLED;
        this.isDeleted = false;
    }

    public void updateInfo(String name, String phoneNumber, String description, StudentStatus status) {
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
    }

    public void delete() {
        this.isDeleted = true;
    }

    public List<Classroom> getClassrooms() {
        return studentClassrooms.stream()
                .filter(studentClassroom -> !studentClassroom.isDeleted())
                .map(StudentClassroom::getClassroom)
                .toList();
    }

    public void setClassrooms(Collection<Classroom> classrooms) {
        Set<Long> targetClassroomIds = new HashSet<>();
        if (classrooms != null) {
            classrooms.stream()
                    .filter(classroom -> classroom != null && classroom.getId() != null)
                    .map(Classroom::getId)
                    .forEach(targetClassroomIds::add);
        }

        studentClassrooms.forEach(studentClassroom -> {
            Long classroomId = studentClassroom.getClassroom().getId();
            if (targetClassroomIds.contains(classroomId)) {
                studentClassroom.restore();
            } else {
                studentClassroom.delete();
            }
        });

        if (classrooms != null) {
            classrooms.stream()
                    .distinct()
                    .forEach(this::addClassroom);
        }
    }

    public void addClassroom(Classroom classroom) {
        if (classroom == null) {
            return;
        }
        studentClassrooms.stream()
                .filter(studentClassroom -> isSameClassroom(studentClassroom.getClassroom(), classroom))
                .findFirst()
                .ifPresentOrElse(StudentClassroom::restore, () -> studentClassrooms.add(new StudentClassroom(this, classroom)));
    }

    private boolean isSameClassroom(Classroom existing, Classroom classroom) {
        return existing == classroom
                || existing.getId() != null && existing.getId().equals(classroom.getId());
    }
}
