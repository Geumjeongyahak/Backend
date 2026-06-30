package geumjeongyahak.domain.student.v1.controller;

import geumjeongyahak.domain.student.enums.StudentStatus;
import geumjeongyahak.domain.student.service.StudentAdminViewService;
import geumjeongyahak.domain.student.service.StudentAdminViewService.StudentFilter;
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
import java.util.List;

@Controller
@RequestMapping("/admin/student/students")
@RequiredArgsConstructor
public class StudentViewController {

    private final StudentAdminViewService studentAdminViewService;

    @GetMapping
    public String students(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            Model model,
            Authentication authentication
    ) {
        StudentFilter filter = new StudentFilter(name, status, classroomId, page, size, sort);
        model.addAttribute("active", "students");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("studentsPage", studentAdminViewService.getStudents(filter));
        model.addAttribute("studentStatuses", studentAdminViewService.getStatuses());
        model.addAttribute("classrooms", studentAdminViewService.getClassroomOptions());
        return "admin/student/students";
    }

    @GetMapping("/new")
    public String newStudent(Model model, Authentication authentication) {
        model.addAttribute("active", "students");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("classrooms", studentAdminViewService.getClassroomOptions());
        return "admin/student/students-form";
    }

    @PostMapping
    public String createStudent(
            @RequestParam String name,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String description,
            @RequestParam List<Long> classroomIds,
            RedirectAttributes redirectAttributes
    ) {
        Long studentId = studentAdminViewService.createStudent(name, phoneNumber, description, classroomIds);
        redirectAttributes.addFlashAttribute("message", "학생을 등록했습니다.");
        return "redirect:/admin/student/students/" + studentId;
    }

    @GetMapping("/{studentId}")
    public String studentDetail(
            @PathVariable Long studentId,
            Model model,
            Authentication authentication
    ) {
        model.addAttribute("active", "students");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("student", studentAdminViewService.getStudent(studentId));
        return "admin/student/students-detail";
    }

    @GetMapping("/{studentId}/edit")
    public String editStudent(
            @PathVariable Long studentId,
            Model model,
            Authentication authentication
    ) {
        model.addAttribute("active", "students");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("student", studentAdminViewService.getStudent(studentId));
        model.addAttribute("studentStatuses", studentAdminViewService.getStatuses());
        model.addAttribute("classrooms", studentAdminViewService.getClassroomOptions());
        return "admin/student/students-edit";
    }

    @PostMapping("/{studentId}")
    public String updateStudent(
            @PathVariable Long studentId,
            @RequestParam String name,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String description,
            @RequestParam StudentStatus status,
            @RequestParam List<Long> classroomIds,
            RedirectAttributes redirectAttributes
    ) {
        studentAdminViewService.updateStudent(studentId, name, phoneNumber, description, status, classroomIds);
        redirectAttributes.addFlashAttribute("message", "학생 정보를 수정했습니다.");
        return "redirect:/admin/student/students/" + studentId;
    }

    @PostMapping("/{studentId}/delete")
    public String deleteStudent(
            @PathVariable Long studentId,
            RedirectAttributes redirectAttributes
    ) {
        studentAdminViewService.deleteStudent(studentId);
        redirectAttributes.addFlashAttribute("message", "학생을 삭제했습니다.");
        return "redirect:/admin/student/students";
    }
}
