package geumjeongyahak.domain.daily_schedule.v1.controller;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleListRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final DailyScheduleService dailyScheduleService;

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @Operation(
        summary = "하루 일정 목록 조회",
        description = "수업 일지 목록 화면에서 사용할 하루 단위 일정을 조회합니다. 캘린더 교시 조회는 Lesson API를 사용합니다."
    )
    @GetMapping
    public ResponseEntity<List<DailyScheduleSummaryResponse>> getDailySchedules(
        @ParameterObject @Valid @ModelAttribute DailyScheduleListRequest request
    ) {
        log.debug(
            "GET /api/v1/daily-schedules - 하루 일정 목록 조회 요청 (from={}, to={}, classroomId={}, teacherId={}, status={})",
            request.from(),
            request.to(),
            request.classroomId(),
            request.teacherId(),
            request.status()
        );
        return ResponseEntity.ok(dailyScheduleService.getDailySchedules(request));
    }

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @Operation(summary = "하루 일정 상세 조회", description = "수업 일지 작성/출석 처리 화면에 필요한 하루 일정 상세 정보를 조회합니다.")
    @GetMapping("/{dailyScheduleId}")
    public ResponseEntity<DailyScheduleDetailResponse> getDailySchedule(
        @PathVariable Long dailyScheduleId
    ) {
        log.debug("GET /api/v1/daily-schedules/{} - 하루 일정 상세 조회 요청", dailyScheduleId);
        return ResponseEntity.ok(dailyScheduleService.getDailySchedule(dailyScheduleId));
    }
}
