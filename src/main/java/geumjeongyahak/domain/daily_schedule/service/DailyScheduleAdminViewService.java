package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleListRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyScheduleAdminViewService {

    private static final int PREVIOUS_WEEK_OFFSET = -1;
    private static final int NEXT_WEEK_OFFSET = 1;

    private final DailyScheduleService dailyScheduleService;
    private final DailyStudentAttendanceRepository dailyStudentAttendanceRepository;
    private final ClassroomProxyService classroomProxyService;
    private final UserProxyService userProxyService;

    public DailyScheduleAdminPage getDailySchedules(DailyScheduleFilter filter) {
        DateRange range = resolveDateRange(filter.from(), filter.to());
        List<DailyScheduleSummaryResponse> summaries = dailyScheduleService.getDailySchedules(
            new DailyScheduleListRequest(
                range.from(),
                range.to(),
                filter.classroomId(),
                filter.teacherId(),
                filter.status()
            )
        );
        Map<Long, StudentAttendanceSummary> attendanceSummaries = getStudentAttendanceSummaries(summaries);
        List<DailyScheduleRow> rows = summaries.stream()
            .map(summary -> DailyScheduleRow.from(
                summary,
                attendanceSummaries.getOrDefault(
                    summary.dailyScheduleId(),
                    StudentAttendanceSummary.empty()
                )
            ))
            .toList();

        return new DailyScheduleAdminPage(
            range.from(),
            range.to(),
            range.shiftWeeks(PREVIOUS_WEEK_OFFSET),
            range.currentWeek(),
            range.shiftWeeks(NEXT_WEEK_OFFSET),
            rows
        );
    }

    public List<ClassroomOption> getClassroomOptions() {
        return classroomProxyService.getActiveClassroomsOrderByName()
            .stream()
            .map(ClassroomOption::from)
            .toList();
    }

    public List<TeacherOption> getTeacherOptions() {
        return userProxyService.getTeacherCandidatesOrderByName()
            .stream()
            .map(TeacherOption::from)
            .toList();
    }

    public DailyScheduleStatus[] getStatuses() {
        return DailyScheduleStatus.values();
    }

    public String getScheduleStatusLabel(DailyScheduleStatus status) {
        return status.getDisplayName();
    }

    public String getTeacherAttendanceStatusLabel(DailyTeacherAttendanceStatus status) {
        return status == null ? "미처리" : status.getDisplayName();
    }

    private DateRange resolveDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return new DateRange(from, to);
        }
        LocalDate baseDate = from != null ? from : LocalDate.now();
        LocalDate weekStart = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new DateRange(weekStart, weekEnd);
    }

    private Map<Long, StudentAttendanceSummary> getStudentAttendanceSummaries(
        List<DailyScheduleSummaryResponse> summaries
    ) {
        List<Long> dailyScheduleIds = summaries.stream()
            .map(DailyScheduleSummaryResponse::dailyScheduleId)
            .toList();
        if (dailyScheduleIds.isEmpty()) {
            return Map.of();
        }

        return dailyStudentAttendanceRepository.findAllByDailySchedule_IdInAndIsDeletedFalse(dailyScheduleIds)
            .stream()
            .collect(Collectors.groupingBy(
                attendance -> attendance.getDailySchedule().getId(),
                Collectors.collectingAndThen(Collectors.toList(), this::toStudentAttendanceSummary)
            ));
    }

    private StudentAttendanceSummary toStudentAttendanceSummary(List<DailyStudentAttendance> attendances) {
        Map<DailyStudentAttendanceStatus, Long> counts = attendances.stream()
            .collect(Collectors.groupingBy(
                DailyStudentAttendance::getStatus,
                () -> new EnumMap<>(DailyStudentAttendanceStatus.class),
                Collectors.counting()
            ));
        return new StudentAttendanceSummary(
            counts.getOrDefault(DailyStudentAttendanceStatus.PRESENT, 0L),
            counts.getOrDefault(DailyStudentAttendanceStatus.LATE, 0L),
            counts.getOrDefault(DailyStudentAttendanceStatus.ABSENT, 0L),
            attendances.size()
        );
    }

    public record DailyScheduleFilter(
        LocalDate from,
        LocalDate to,
        Long classroomId,
        Long teacherId,
        DailyScheduleStatus status
    ) {
    }

    public record DailyScheduleAdminPage(
        LocalDate from,
        LocalDate to,
        DateRange previousWeek,
        DateRange currentWeek,
        DateRange nextWeek,
        List<DailyScheduleRow> rows
    ) {
    }

    public record DateRange(
        LocalDate from,
        LocalDate to
    ) {
        private DateRange shiftWeeks(int weekOffset) {
            return new DateRange(from.plusWeeks(weekOffset), to.plusWeeks(weekOffset));
        }

        private DateRange currentWeek() {
            LocalDate today = LocalDate.now();
            return new DateRange(
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            );
        }
    }

    public record DailyScheduleRow(
        Long dailyScheduleId,
        LocalDate lessonDate,
        Long classroomId,
        String classroomName,
        Long teacherId,
        String teacherName,
        LocalTime activityStartTime,
        LocalTime activityEndTime,
        Integer volunteerServiceMinutes,
        DailyScheduleStatus status,
        String statusLabel,
        DailyTeacherAttendanceStatus teacherAttendanceStatus,
        String teacherAttendanceStatusLabel,
        int lessonCount,
        StudentAttendanceSummary studentAttendanceSummary
    ) {
        private static DailyScheduleRow from(
            DailyScheduleSummaryResponse summary,
            StudentAttendanceSummary studentAttendanceSummary
        ) {
            return new DailyScheduleRow(
                summary.dailyScheduleId(),
                summary.lessonDate(),
                summary.classroomId(),
                summary.classroomName(),
                summary.teacherId(),
                summary.teacherName(),
                summary.activityStartTime(),
                summary.activityEndTime(),
                summary.volunteerServiceMinutes(),
                summary.status(),
                summary.status().getDisplayName(),
                summary.teacherAttendanceStatus(),
                summary.teacherAttendanceStatus() == null
                    ? "미처리"
                    : summary.teacherAttendanceStatus().getDisplayName(),
                summary.lessonCount(),
                studentAttendanceSummary
            );
        }
    }

    public record StudentAttendanceSummary(
        long presentCount,
        long lateCount,
        long absentCount,
        long totalCount
    ) {
        private static StudentAttendanceSummary empty() {
            return new StudentAttendanceSummary(0L, 0L, 0L, 0L);
        }
    }

    public record ClassroomOption(
        Long id,
        String name
    ) {
        private static ClassroomOption from(Classroom classroom) {
            return new ClassroomOption(classroom.getId(), classroom.getName());
        }
    }

    public record TeacherOption(
        Long id,
        String name
    ) {
        private static TeacherOption from(User user) {
            return new TeacherOption(user.getId(), user.getName());
        }
    }
}
