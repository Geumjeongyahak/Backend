package geumjeongyahak.domain.daily_schedule.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "daily_teacher_attendances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyTeacherAttendance extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_schedule_id", nullable = false)
    private DailySchedule dailySchedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DailyTeacherAttendanceStatus status;

    @Column(name = "volunteer_service_minutes")
    private Integer volunteerServiceMinutes;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public DailyTeacherAttendance(DailySchedule dailySchedule, Integer volunteerServiceMinutes) {
        this.dailySchedule = dailySchedule;
        this.volunteerServiceMinutes = volunteerServiceMinutes;
        this.status = DailyTeacherAttendanceStatus.ABSENT;
        this.isDeleted = false;
    }

    public void updateVolunteerServiceMinutes(Integer volunteerServiceMinutes) {
        this.volunteerServiceMinutes = volunteerServiceMinutes;
    }

    public void updateAttendance(
        DailyTeacherAttendanceStatus status,
        LocalDateTime attendedAt,
        BigDecimal latitude,
        BigDecimal longitude
    ) {
        this.status = status;
        this.attendedAt = attendedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        if (attendedAt == null) {
            this.checkedOutAt = null;
        }
    }

    public boolean isAttended() {
        return attendedAt != null;
    }

    public boolean isCheckedOut() {
        return checkedOutAt != null;
    }

    public void checkOut(LocalDateTime checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public void correctAttendance(
        DailyTeacherAttendanceStatus status,
        LocalDateTime attendedAt,
        LocalDateTime checkedOutAt
    ) {
        this.status = status;
        this.attendedAt = attendedAt;
        this.checkedOutAt = checkedOutAt;
        if (attendedAt == null) {
            this.latitude = null;
            this.longitude = null;
        }
    }

    public void restore() {
        this.isDeleted = false;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    /** 활동 시작~종료 시각 사이의 봉사 시간(분). 둘 중 하나라도 없으면 null. */
    public static Integer volunteerMinutesBetween(LocalTime activityStartTime, LocalTime activityEndTime) {
        if (activityStartTime == null || activityEndTime == null) {
            return null;
        }
        return Math.toIntExact(Duration.between(activityStartTime, activityEndTime).toMinutes());
    }
}
