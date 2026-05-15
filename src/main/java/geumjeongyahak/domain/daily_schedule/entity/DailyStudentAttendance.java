package geumjeongyahak.domain.daily_schedule.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import geumjeongyahak.domain.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "daily_student_attendances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyStudentAttendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_schedule_id", nullable = false)
    private DailySchedule dailySchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DailyStudentAttendanceStatus status;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public DailyStudentAttendance(DailySchedule dailySchedule, Student student) {
        this.dailySchedule = dailySchedule;
        this.student = student;
        this.status = DailyStudentAttendanceStatus.ABSENT;
        this.isDeleted = false;
    }

    public void updateStatus(DailyStudentAttendanceStatus status) {
        this.status = status;
    }

    public void restore() {
        this.isDeleted = false;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
