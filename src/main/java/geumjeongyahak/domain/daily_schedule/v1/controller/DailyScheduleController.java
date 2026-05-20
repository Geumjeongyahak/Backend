package geumjeongyahak.domain.daily_schedule.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailySchedulePaginationRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleVolunteerHoursRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleJournalRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyStudentAttendancesRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleSummaryResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleVolunteerHoursResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/daily-schedules")
@RequiredArgsConstructor
@Tag(name = "DailySchedule", description = "하루 일정 및 수업 일지 API")
public class DailyScheduleController {

    private static final String DAILY_SCHEDULE_READ_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";
    private static final String DAILY_SCHEDULE_WRITE_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN') or hasAuthority('daily-schedule:manage:*')";

    private final DailyScheduleService dailyScheduleService;

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @Operation(
        summary = "하루 일정 목록 조회",
        description = "수업 일지 목록 화면에서 사용할 하루 단위 일정을 조회합니다. 캘린더 교시 조회는 Lesson API를 사용합니다."
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<DailyScheduleSummaryResponse>> getDailySchedules(
        @ParameterObject @Valid @ModelAttribute DailySchedulePaginationRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "GET /api/v1/daily-schedules - 수업 일지 목록 조회 요청 (keyword={}, mine={}, page={}, size={})",
            request.getKeyword(),
            request.getMine(),
            request.getPage(),
            request.getSize()
        );
        return ResponseEntity.ok(dailyScheduleService.getJournalDailySchedules(request, userDetails.getUserId()));
    }

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @Operation(summary = "하루 일정 상세 조회", description = "수업 일지 작성/출석 처리 화면에 필요한 하루 일정 상세 정보를 조회합니다.")
    @GetMapping("/{dailyScheduleId}")
    public ResponseEntity<DailyScheduleDetailResponse> getDailySchedule(
        @PathVariable Long dailyScheduleId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/daily-schedules/{} - 하루 일정 상세 조회 요청", dailyScheduleId);
        return ResponseEntity.ok(dailyScheduleService.getDailySchedule(
            dailyScheduleId,
            userDetails.getUserId(),
            canViewSensitiveInfo(userDetails)
        ));
    }

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @Operation(
        summary = "봉사 시간 조회",
        description = "완료된 하루 일정과 교사 출석을 기준으로 봉사 인정 시간을 조회합니다. 날짜를 입력하지 않으면 전체 누적 봉사시간을 조회합니다."
    )
    @GetMapping("/volunteer-hours")
    public ResponseEntity<DailyScheduleVolunteerHoursResponse> getVolunteerHours(
        @ParameterObject @Valid @ModelAttribute DailyScheduleVolunteerHoursRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "GET /api/v1/daily-schedules/volunteer-hours - 봉사 시간 조회 요청 (teacherId={}, from={}, to={})",
            request.teacherId(),
            request.from(),
            request.to()
        );
        return ResponseEntity.ok(dailyScheduleService.getVolunteerHours(
            userDetails.getUserId(),
            canViewSensitiveInfo(userDetails),
            request
        ));
    }

    @PreAuthorize(DAILY_SCHEDULE_WRITE_ACCESS)
    @Operation(summary = "하루 일정 수업 일지 저장", description = "하루 일정에 연결된 교시별 수업 일지를 저장하거나 수정합니다.")
    @PutMapping("/{dailyScheduleId}/journal")
    public ResponseEntity<DailyScheduleDetailResponse> updateJournal(
        @PathVariable Long dailyScheduleId,
        @Valid @RequestBody UpdateDailyScheduleJournalRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "PUT /api/v1/daily-schedules/{}/journal - 수업 일지 저장 요청 (lessonJournalCount={})",
            dailyScheduleId,
            request.lessonJournals().size()
        );
        return ResponseEntity.ok(dailyScheduleService.updateJournal(
            dailyScheduleId,
            userDetails.getUserId(),
            canManageAnyDailySchedule(userDetails),
            request
        ));
    }

    @PreAuthorize(DAILY_SCHEDULE_WRITE_ACCESS)
    @Operation(summary = "하루 일정 학생 출석 처리", description = "하루 일정에 연결된 학생들의 출석 상태를 처리합니다.")
    @PatchMapping("/{dailyScheduleId}/student-attendances")
    public ResponseEntity<DailyScheduleDetailResponse> updateStudentAttendances(
        @PathVariable Long dailyScheduleId,
        @Valid @RequestBody UpdateDailyStudentAttendancesRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "PATCH /api/v1/daily-schedules/{}/student-attendances - 학생 출석 처리 요청 (attendanceCount={})",
            dailyScheduleId,
            request.attendances().size()
        );
        return ResponseEntity.ok(dailyScheduleService.updateStudentAttendances(
            dailyScheduleId,
            userDetails.getUserId(),
            canManageAnyDailySchedule(userDetails),
            canViewSensitiveInfo(userDetails),
            request
        ));
    }

    @PreAuthorize(DAILY_SCHEDULE_WRITE_ACCESS)
    @Operation(summary = "하루 일정 교사 출석 처리", description = "하루 일정의 교사 출석 상태를 처리합니다.")
    @PatchMapping("/{dailyScheduleId}/teacher-attendance")
    public ResponseEntity<DailyScheduleDetailResponse> updateTeacherAttendance(
        @PathVariable Long dailyScheduleId,
        @Valid @RequestBody UpdateDailyTeacherAttendanceRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "PATCH /api/v1/daily-schedules/{}/teacher-attendance - 교사 출석 처리 요청 (status={})",
            dailyScheduleId,
            request.status()
        );
        return ResponseEntity.ok(dailyScheduleService.updateTeacherAttendance(
            dailyScheduleId,
            userDetails.getUserId(),
            canManageAnyDailySchedule(userDetails),
            canViewSensitiveInfo(userDetails),
            request
        ));
    }

    private boolean canManageAnyDailySchedule(CustomUserDetails userDetails) {
        return userDetails.isAdmin()
            || userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("daily-schedule:manage:*"::equals);
    }

    private boolean canViewSensitiveInfo(CustomUserDetails userDetails) {
        return userDetails.isAdmin()
            || userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "daily-schedule:read:*".equals(authority)
                    || "daily-schedule:manage:*".equals(authority));
    }
}
