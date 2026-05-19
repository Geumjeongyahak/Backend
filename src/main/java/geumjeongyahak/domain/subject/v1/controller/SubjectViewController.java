package geumjeongyahak.domain.subject.v1.controller;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.subject.service.SubjectAdminViewService;
import geumjeongyahak.domain.subject.service.SubjectAdminViewService.SubjectFilter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/subject/subjects")
@RequiredArgsConstructor
public class SubjectViewController {

    private static final String SUBJECT_READ_ACCESS =
        "hasRole('ADMIN') or hasAuthority('subject:read:*')";
    private static final String SUBJECT_WRITE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('subject:write:*')";
    private static final String SUBJECT_MANAGE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('subject:manage:*')";

    private final SubjectAdminViewService subjectAdminViewService;

    @PreAuthorize(SUBJECT_READ_ACCESS)
    @GetMapping
    public String subjects(
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Boolean active,
        Model model,
        Authentication authentication
    ) {
        SubjectFilter filter = new SubjectFilter(classroomId, active);
        model.addAttribute("active", "subjects");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("subjectsPage", subjectAdminViewService.getSubjects(filter));
        model.addAttribute("classrooms", subjectAdminViewService.getClassroomOptions());
        return "admin/subject/subjects";
    }

    @PreAuthorize(SUBJECT_READ_ACCESS)
    @GetMapping("/{subjectId}")
    public String subjectDetail(
        @PathVariable Long subjectId,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Boolean active,
        Model model,
        Authentication authentication
    ) {
        SubjectFilter filter = new SubjectFilter(classroomId, active);
        model.addAttribute("active", "subjects");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("subject", subjectAdminViewService.getSubject(subjectId));
        model.addAttribute("teachers", subjectAdminViewService.getTeacherOptions());
        model.addAttribute("dayOfWeeks", subjectAdminViewService.getDayOfWeekOptions());
        return "admin/subject/subjects-detail";
    }

    @PreAuthorize(SUBJECT_WRITE_ACCESS)
    @GetMapping("/new")
    public String newSubject(Model model, Authentication authentication) {
        addSubjectFormAttributes(model, authentication);
        return "admin/subject/subjects-form";
    }

    @PreAuthorize(SUBJECT_WRITE_ACCESS)
    @PostMapping
    public String createSubject(
            @RequestParam Long classroomId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam String name,
            @RequestParam LocalDate startAt,
            @RequestParam LocalDate endAt,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime,
            @RequestParam Integer period,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        Long subjectId = subjectAdminViewService.createSubject(
                classroomId,
                teacherId,
                name,
                startAt,
                endAt,
                dayOfWeek,
                startTime,
                endTime,
                period,
                description
        );
        redirectAttributes.addFlashAttribute("message", "과목을 등록했습니다.");
        return "redirect:/admin/subject/subjects/" + subjectId;
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @GetMapping("/{subjectId}/edit")
    public String editSubject(
            @PathVariable Long subjectId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) Boolean active,
            Model model,
            Authentication authentication
    ) {
        SubjectFilter filter = new SubjectFilter(classroomId, active);
        model.addAttribute("active", "subjects");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("subject", subjectAdminViewService.getSubject(subjectId));
        return "admin/subject/subjects-edit";
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @PostMapping("/{subjectId}")
    public String updateSubject(
        @PathVariable Long subjectId,
        @RequestParam String name,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Boolean active,
        RedirectAttributes redirectAttributes
    ) {
        subjectAdminViewService.updateSubject(subjectId, name, description);
        redirectAttributes.addFlashAttribute("message", "과목 기본 정보를 수정했습니다.");
        addRedirectAttributeIfPresent(redirectAttributes, "classroomId", classroomId);
        addRedirectAttributeIfPresent(redirectAttributes, "active", active);
        return "redirect:/admin/subject/subjects/" + subjectId;
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @PostMapping("/{subjectId}/teacher")
    public String assignTeacher(
        @PathVariable Long subjectId,
        @RequestParam(required = false) Long teacherId,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Boolean active,
        RedirectAttributes redirectAttributes
    ) {
        try {
            subjectAdminViewService.assignTeacher(subjectId, teacherId);
            redirectAttributes.addFlashAttribute("message", "담당 교사를 변경했습니다.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        addRedirectAttributeIfPresent(redirectAttributes, "classroomId", classroomId);
        addRedirectAttributeIfPresent(redirectAttributes, "active", active);
        return "redirect:/admin/subject/subjects/" + subjectId;
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @PostMapping("/{subjectId}/schedule")
    public String updateSchedule(
        @PathVariable Long subjectId,
        @RequestParam LocalDate startAt,
        @RequestParam LocalDate endAt,
        @RequestParam DayOfWeek dayOfWeek,
        @RequestParam LocalTime startTime,
        @RequestParam LocalTime endTime,
        @RequestParam Integer period,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Boolean active,
        RedirectAttributes redirectAttributes
    ) {
        try {
            subjectAdminViewService.updateSchedule(
                subjectId,
                startAt,
                endAt,
                dayOfWeek,
                startTime,
                endTime,
                period
            );
            redirectAttributes.addFlashAttribute("message", "과목 일정을 수정했습니다.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        addRedirectAttributeIfPresent(redirectAttributes, "classroomId", classroomId);
        addRedirectAttributeIfPresent(redirectAttributes, "active", active);
        return "redirect:/admin/subject/subjects/" + subjectId;
    }

    @PreAuthorize(SUBJECT_MANAGE_ACCESS)
    @PostMapping("/{subjectId}/deactivate")
    public String deactivateSubject(
        @PathVariable Long subjectId,
        @RequestParam(required = false) Long classroomId,
        @RequestParam(required = false) Boolean active,
        RedirectAttributes redirectAttributes
    ) {
        subjectAdminViewService.deactivateSubject(subjectId);
        redirectAttributes.addFlashAttribute("message", "과목을 비활성화했습니다.");
        addRedirectAttributeIfPresent(redirectAttributes, "classroomId", classroomId);
        addRedirectAttributeIfPresent(redirectAttributes, "active", active);
        return "redirect:/admin/subject/subjects/" + subjectId;
    }

    private void addSubjectFormAttributes(Model model, Authentication authentication) {
        model.addAttribute("active", "subjects");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("classrooms", subjectAdminViewService.getClassroomOptions());
        model.addAttribute("teachers", subjectAdminViewService.getTeacherOptions());
        model.addAttribute("dayOfWeeks", subjectAdminViewService.getDayOfWeekOptions());
    }

    private void addRedirectAttributeIfPresent(RedirectAttributes redirectAttributes, String name, Object value) {
        if (value != null) {
            redirectAttributes.addAttribute(name, value.toString());
        }
    }
}
