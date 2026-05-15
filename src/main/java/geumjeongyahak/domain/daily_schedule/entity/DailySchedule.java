package geumjeongyahak.domain.daily_schedule.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.users.entity.User;
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

@Entity
@Getter
@Table(name = "daily_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "activity_start_time")
    private LocalTime activityStartTime;

    @Column(name = "activity_end_time")
    private LocalTime activityEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DailyScheduleStatus status;

    public DailySchedule(
        Classroom classroom,
        User teacher,
        LocalDate lessonDate,
        LocalTime activityStartTime,
        LocalTime activityEndTime
    ) {
        this.classroom = classroom;
        this.teacher = teacher;
        this.lessonDate = lessonDate;
        this.activityStartTime = activityStartTime;
        this.activityEndTime = activityEndTime;
        this.status = DailyScheduleStatus.SCHEDULED;
    }

    public void updateTeacher(User teacher) {
        this.teacher = teacher;
    }

    public void updateActivityTime(LocalTime activityStartTime, LocalTime activityEndTime) {
        this.activityStartTime = activityStartTime;
        this.activityEndTime = activityEndTime;
    }

    public void updateStatus(DailyScheduleStatus status) {
        this.status = status;
    }
}
