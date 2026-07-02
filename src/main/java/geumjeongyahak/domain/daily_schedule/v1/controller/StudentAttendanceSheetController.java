package geumjeongyahak.domain.daily_schedule.v1.controller;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.StudentAttendanceSheetRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.StudentAttendanceSheetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/attendance-sheets")
@RequiredArgsConstructor
@Tag(name = "AttendanceSheet", description = "학생 출석부 API")
public class StudentAttendanceSheetController {

    private static final String DAILY_SCHEDULE_READ_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";

    private final DailyScheduleService dailyScheduleService;

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @Operation(
        summary = "월간 학생 출석부 조회",
        description = """
            특정 연월과 분반을 기준으로 학생 출석부 데이터를 조회합니다.

            응답은 시트 생성을 위해 학생 목록과 날짜별 수업 일정 목록을 분리해 반환합니다.
            학생 목록은 행 방향, 일정 목록은 열 방향으로 사용할 수 있으며,
            각 일정의 studentAttendances에는 학생별 출석 상태와 출석 식별자가 포함됩니다.
            """
    )
    @GetMapping
    public ResponseEntity<StudentAttendanceSheetResponse> getStudentAttendanceSheet(
        @ParameterObject @Valid @ModelAttribute StudentAttendanceSheetRequest request
    ) {
        log.debug(
            "GET /api/v1/attendance-sheets - 월간 학생 출석부 조회 요청 (year={}, month={}, classroomId={})",
            request.year(),
            request.month(),
            request.classroomId()
        );
        return ResponseEntity.ok(dailyScheduleService.getStudentAttendanceSheet(request));
    }
}
