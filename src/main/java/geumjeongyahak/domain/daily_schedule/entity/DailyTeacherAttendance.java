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
import java.time.LocalDateTime;
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
    }

    public void restore() {
        this.isDeleted = false;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
