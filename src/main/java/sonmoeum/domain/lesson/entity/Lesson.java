package sonmoeum.domain.lesson.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.users.entity.User;

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

@Entity
@Getter
@Table(name = "lessons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus attendance;

    public enum AttendanceStatus {
        PENDING,
        PRESENT,
        ABSENT,
        EXCUSED // 공결 (사유 있는 결석)
    }

    public Lesson(
            Subject subject,
            User teacher,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            AttendanceStatus attendance) {
        this.subject = subject;
        this.teacher = teacher;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendance = attendance;
    }


    public void updateAttendance(AttendanceStatus attendance) {
        this.attendance = attendance;
    }
}
