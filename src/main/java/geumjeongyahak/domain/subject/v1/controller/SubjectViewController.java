package geumjeongyahak.domain.subject.v1.controller;

import geumjeongyahak.domain.subject.service.SubjectAdminViewService;
import geumjeongyahak.domain.subject.service.SubjectAdminViewService.SubjectFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/subject/subjects")
@RequiredArgsConstructor
public class SubjectViewController {

    private static final String SUBJECT_READ_ACCESS =
        "hasRole('ADMIN') or hasAuthority('subject:read:*')";

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
        return "admin/subject/subjects-detail";
    }
}
