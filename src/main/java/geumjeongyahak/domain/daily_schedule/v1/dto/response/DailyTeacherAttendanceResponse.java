package geumjeongyahak.domain.daily_schedule.v1.dto.response;

import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DailyTeacherAttendanceResponse(
    @Schema(description = "교사 출석 식별자", example = "1")
    Long attendanceId,

    @Schema(
        description = "교사 출석 상태",
        example = "ABSENT",
        allowableValues = {"PRESENT", "ABSENT", "LATE", "EXCUSED"}
    )
    DailyTeacherAttendanceStatus status,

    @Schema(description = "출석 처리 시각. 출석 상태가 ABSENT이면 null입니다.", example = "2026-06-20T14:00:00")
    LocalDateTime attendedAt,

    @Schema(description = "출석 처리 여부", example = "true")
    boolean isAttended,

    @Schema(description = "퇴근 처리 시각. 퇴근 처리 전이면 null입니다.", example = "2026-06-20T16:00:00")
    LocalDateTime checkedOutAt,

    @Schema(description = "퇴근 처리 여부", example = "false")
    boolean isCheckedOut,

    @Schema(description = "출석 처리 위치 위도. 출석 상태가 ABSENT이면 null입니다.", example = "35.1795543")
    BigDecimal latitude,

    @Schema(description = "출석 처리 위치 경도. 출석 상태가 ABSENT이면 null입니다.", example = "129.0756416")
    BigDecimal longitude,

    @Schema(description = "봉사 인정 시간(분)", example = "120")
    Integer volunteerServiceMinutes
) {

    public static DailyTeacherAttendanceResponse from(DailyTeacherAttendance attendance) {
        if (attendance == null) {
            return null;
        }
        return new DailyTeacherAttendanceResponse(
            attendance.getId(),
            attendance.getStatus(),
            attendance.getAttendedAt(),
            attendance.isAttended(),
            attendance.getCheckedOutAt(),
            attendance.isCheckedOut(),
            attendance.getLatitude(),
            attendance.getLongitude(),
            attendance.getVolunteerServiceMinutes()
        );
    }
}
