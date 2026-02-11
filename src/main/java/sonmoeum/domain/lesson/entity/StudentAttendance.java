package sonmoeum.domain.lesson.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.lesson.enums.StudentAttendanceStatus;
import sonmoeum.domain.student.entity.Student;

@Entity
@Getter
@Table(name = "student_attendances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentAttendanceStatus status;

    @Column(length = 255)
    private String memo;

    public StudentAttendance(Lesson lesson, Student student) {
        this.lesson = lesson;
        this.student = student;
        this.status = StudentAttendanceStatus.ABSENT;
    }

    public void updateStatus(StudentAttendanceStatus status) {
        this.status = status;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
