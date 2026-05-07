package geumjeongyahak.domain.classroom.v1.controller;

import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.service.ClassroomAdminViewService;
import geumjeongyahak.domain.classroom.service.ClassroomAdminViewService.ClassroomFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/classroom/classrooms")
@RequiredArgsConstructor
public class ClassroomViewController {

    private final ClassroomAdminViewService classroomAdminViewService;

    @GetMapping
    public String classrooms(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) ClassroomType type,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        ClassroomFilter filter = new ClassroomFilter(name, type, sort);
        model.addAttribute("active", "classrooms");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("classrooms", classroomAdminViewService.getClassrooms(filter));
        model.addAttribute("classroomTypes", classroomAdminViewService.getClassroomTypes());
        return "admin/classroom/classrooms";
    }

    @GetMapping("/new")
    public String newClassroom(Model model, Authentication authentication) {
        model.addAttribute("active", "classrooms");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("classroomTypes", classroomAdminViewService.getClassroomTypes());
        return "admin/classroom/classrooms-form";
    }

    @PostMapping
    public String createClassroom(
        @RequestParam String name,
        @RequestParam String type,
        @RequestParam(required = false) String description,
        RedirectAttributes redirectAttributes
    ) {
        Long classroomId = classroomAdminViewService.createClassroom(name, type, description);
        redirectAttributes.addFlashAttribute("message", "분반을 등록했습니다.");
        return "redirect:/admin/classroom/classrooms/" + classroomId;
    }

    @GetMapping("/{classroomId}")
    public String classroomDetail(
        @PathVariable Long classroomId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "classrooms");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("classroom", classroomAdminViewService.getClassroom(classroomId));
        return "admin/classroom/classrooms-detail";
    }

    @GetMapping("/{classroomId}/edit")
    public String editClassroom(
        @PathVariable Long classroomId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "classrooms");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("classroom", classroomAdminViewService.getClassroom(classroomId));
        model.addAttribute("classroomTypes", classroomAdminViewService.getClassroomTypes());
        return "admin/classroom/classrooms-edit";
    }

    @PostMapping("/{classroomId}")
    public String updateClassroom(
        @PathVariable Long classroomId,
        @RequestParam String name,
        @RequestParam String type,
        @RequestParam(required = false) String description,
        RedirectAttributes redirectAttributes
    ) {
        classroomAdminViewService.updateClassroom(classroomId, name, type, description);
        redirectAttributes.addFlashAttribute("message", "분반 정보를 수정했습니다.");
        return "redirect:/admin/classroom/classrooms/" + classroomId;
    }
}
