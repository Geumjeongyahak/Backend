package geumjeongyahak.domain.daily_schedule.v1.controller;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminViewService;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleAdminViewService.DailyScheduleFilter;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/daily-schedule/daily-schedules")
@RequiredArgsConstructor
public class DailyScheduleViewController {

    private static final String DAILY_SCHEDULE_READ_ACCESS =
        "hasRole('ADMIN') or hasAuthority('daily-schedule:read:*') or hasAuthority('daily-schedule:manage:*')";
    private static final String DAILY_SCHEDULE_MANAGE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('daily-schedule:manage:*')";

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

    @PreAuthorize(DAILY_SCHEDULE_READ_ACCESS)
    @GetMapping("/{dailyScheduleId}")
    public String dailyScheduleDetail(
        @PathVariable Long dailyScheduleId,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Long teacherId,
        @RequestParam(required = false) DailyScheduleStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication
    ) {
        DailyScheduleFilter filter = new DailyScheduleFilter(from, to, classroomId, teacherId, status);
        model.addAttribute("active", "dailySchedules");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("schedule", dailyScheduleAdminViewService.getDailySchedule(
            userDetails.getUserId(),
            dailyScheduleId
        ));
        model.addAttribute("statuses", dailyScheduleAdminViewService.getStatuses());
        return "admin/daily-schedule/daily-schedules-detail";
    }

    @PreAuthorize(DAILY_SCHEDULE_MANAGE_ACCESS)
    @PostMapping("/{dailyScheduleId}/status")
    public String updateStatus(
        @PathVariable Long dailyScheduleId,
        @RequestParam DailyScheduleStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Long teacherId,
        @RequestParam(required = false) DailyScheduleStatus filterStatus,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        dailyScheduleAdminViewService.updateStatus(userDetails.getUserId(), dailyScheduleId, status);
        redirectAttributes.addFlashAttribute("message", "하루 일정 상태를 변경했습니다.");
        addRedirectAttributeIfPresent(redirectAttributes, "from", from);
        addRedirectAttributeIfPresent(redirectAttributes, "to", to);
        addRedirectAttributeIfPresent(redirectAttributes, "classroomId", classroomId);
        addRedirectAttributeIfPresent(redirectAttributes, "teacherId", teacherId);
        addRedirectAttributeIfPresent(redirectAttributes, "status", filterStatus);
        return "redirect:/admin/daily-schedule/daily-schedules/" + dailyScheduleId;
    }

    private void addRedirectAttributeIfPresent(RedirectAttributes redirectAttributes, String name, Object value) {
        if (value != null) {
            redirectAttributes.addAttribute(name, value.toString());
        }
    }
}
