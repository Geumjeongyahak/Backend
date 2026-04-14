package geumjeongyahak.domain.lesson.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.enums.TeacherAttendanceStatus;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;

@Entity
@Getter
@Table(name = "lessons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private Integer period;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherAttendanceStatus teacherAttendance;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    public Lesson(
        Subject subject,
        User teacher,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        this.subject = subject;
        this.teacher = teacher;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.period = period;
        this.status = LessonStatus.SCHEDULED;
        this.teacherAttendance = TeacherAttendanceStatus.ABSENT;
    }

    public void update(
        Subject subject,
        User teacher,
        LocalDate newDate,
        LocalTime newStart,
        LocalTime newEnd,
        Integer newPeriod
    ) {
        this.subject = subject;
        this.teacher = teacher;
        this.date = newDate;
        this.startTime = newStart;
        this.endTime = newEnd;
        this.period = newPeriod;
    }

    public void updateTeacherAttendance(TeacherAttendanceStatus teacherAttendance) {
        this.teacherAttendance = teacherAttendance;
    }

    public void updateStatus(LessonStatus status) {
        this.status = status;
    }

    public void updateNote(String note) {
        this.note = note;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public void changeTeacher(User newTeacher) {
        this.teacher = newTeacher;
    }
}
