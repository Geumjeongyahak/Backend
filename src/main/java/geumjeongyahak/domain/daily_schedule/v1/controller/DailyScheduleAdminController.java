package geumjeongyahak.domain.daily_schedule.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleStatusRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceCorrectionRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleJournalSheetLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/daily-schedules")
@RequiredArgsConstructor
@Tag(name = "DailySchedule", description = "하루 일정 관리자 API")
public class DailyScheduleAdminController {

    private static final String DAILY_SCHEDULE_MANAGE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('daily-schedule:manage:*')";

    private final DailyScheduleAdminService dailyScheduleAdminService;

    @PreAuthorize(DAILY_SCHEDULE_MANAGE_ACCESS)
    @Operation(
        summary = "수업일지 관리 시트 링크 조회",
        description = """
            봉사시간 증빙용 수업일지 관리 Google Sheets 링크를 반환합니다.

            이 API는 월별 수업일지 시트를 직접 생성하거나 특정 월 시트 링크를 반환하지 않습니다.
            월별 수업일지 생성 및 갱신은 Google Sheets Apps Script 메뉴의
            '월 수업일지 목록 가져오기' 기능을 사용합니다.
            """
    )
    @GetMapping("/journal-sheet-link")
    public ResponseEntity<DailyScheduleJournalSheetLinkResponse> getJournalSheetLink() {
        log.debug("GET /api/v1/daily-schedules/journal-sheet-link - 관리자 수업일지 관리 시트 링크 조회 요청");
        return ResponseEntity.ok(dailyScheduleAdminService.getJournalSheetLink());
    }

    @PreAuthorize(DAILY_SCHEDULE_MANAGE_ACCESS)
    @Operation(
        summary = "하루 일정 상태 변경",
        description = "관리자가 하루 일정 상태를 수동으로 변경합니다. 상태 변경 시 같은 날짜/분반의 활성 Lesson 상태도 함께 연동됩니다."
    )
    @PatchMapping("/{dailyScheduleId}/status")
    public ResponseEntity<DailyScheduleDetailResponse> updateStatus(
        @PathVariable Long dailyScheduleId,
        @Valid @RequestBody UpdateDailyScheduleStatusRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "PATCH /api/v1/daily-schedules/{}/status - 상태 변경 요청 (status={})",
            dailyScheduleId,
            request.status()
        );
        return ResponseEntity.ok(dailyScheduleAdminService.updateStatus(
            dailyScheduleId,
            userDetails.getUserId(),
            canViewSensitiveInfo(userDetails),
            request
        ));
    }

    @PreAuthorize(DAILY_SCHEDULE_MANAGE_ACCESS)
    @Operation(
        summary = "교사 출석 보정",
        description = "관리자가 하루 일정의 교사 출석 상태, 출근 시간, 퇴근 시간을 보정합니다."
    )
    @PatchMapping("/{dailyScheduleId}/teacher-attendance/adjustment")
    public ResponseEntity<DailyScheduleDetailResponse> correctTeacherAttendance(
        @PathVariable Long dailyScheduleId,
        @Valid @RequestBody UpdateDailyTeacherAttendanceCorrectionRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "PATCH /api/v1/daily-schedules/{}/teacher-attendance/adjustment - 관리자 교사 출석 보정 요청 (status={}, attendedAt={}, checkedOutAt={})",
            dailyScheduleId,
            request.status(),
            request.attendedAt(),
            request.checkedOutAt()
        );
        return ResponseEntity.ok(dailyScheduleAdminService.correctTeacherAttendance(
            dailyScheduleId,
            userDetails.getUserId(),
            canViewSensitiveInfo(userDetails),
            request
        ));
    }

    private boolean canViewSensitiveInfo(CustomUserDetails userDetails) {
        return userDetails.isAdmin()
            || userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "daily-schedule:read:*".equals(authority)
                    || "daily-schedule:manage:*".equals(authority));
    }
}
