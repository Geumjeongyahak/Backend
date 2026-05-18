package geumjeongyahak.domain.daily_schedule.v1.controller;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminViewService;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminViewService.DailyScheduleFilter;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/daily-schedule/daily-schedules")
@RequiredArgsConstructor
public class DailyScheduleViewController {

    private static final String DAILY_SCHEDULE_READ_ACCESS =
        "hasRole('ADMIN') or hasAuthority('daily-schedule:read:*') or hasAuthority('daily-schedule:manage:*')";

    private final DailyScheduleAdminViewService dailyScheduleAdminViewService;

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @GetMapping
    public String dailySchedules(
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Long teacherId,
        @RequestParam(required = false) DailyScheduleStatus status,
        Model model,
        Authentication authentication
    ) {
        DailyScheduleFilter filter = new DailyScheduleFilter(from, to, classroomId, teacherId, status);
        model.addAttribute("active", "dailySchedules");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("dailySchedulesPage", dailyScheduleAdminViewService.getDailySchedules(filter));
        model.addAttribute("classrooms", dailyScheduleAdminViewService.getClassroomOptions());
        model.addAttribute("teachers", dailyScheduleAdminViewService.getTeacherOptions());
        model.addAttribute("statuses", dailyScheduleAdminViewService.getStatuses());
        return "admin/daily-schedule/daily-schedules";
    }
}
