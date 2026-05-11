package geumjeongyahak.domain.lesson.v1.controller;

import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.enums.TeacherAttendanceStatus;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService;
import geumjeongyahak.domain.lesson.service.LessonAdminViewService.LessonFilter;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Controller
@RequestMapping("/admin/lesson/lessons")
@RequiredArgsConstructor
public class LessonViewController {

    private final LessonAdminViewService lessonAdminViewService;

    @GetMapping
    public String lessons(
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate endDate,
        @RequestParam(required = false) LessonStatus status,
        @RequestParam(required = false) TeacherAttendanceStatus teacherAttendance,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        LessonFilter filter = new LessonFilter(startDate, endDate, status, teacherAttendance, page, size, sort);
        model.addAttribute("active", "lessons");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("lessonsPage", lessonAdminViewService.getLessons(filter));
        model.addAttribute("lessonStatuses", lessonAdminViewService.getStatuses());
        model.addAttribute("teacherAttendanceStatuses", lessonAdminViewService.getTeacherAttendanceStatuses());
        return "admin/lesson/lessons";
    }
}
