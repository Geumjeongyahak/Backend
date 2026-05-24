package geumjeongyahak.domain.teacher_application.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import geumjeongyahak.domain.teacher_application.service.TeacherApplicationAdminViewService;
import geumjeongyahak.domain.teacher_application.service.TeacherApplicationAdminViewService.TeacherApplicationFilter;
import geumjeongyahak.domain.teacher_application.v1.dto.request.ApproveTeacherApplicationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.request.RejectTeacherApplicationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/request/teacher-applications")
@RequiredArgsConstructor
public class TeacherApplicationViewController {

    private static final String TEACHER_APPLICATION_READ_ACCESS =
        "hasRole('ADMIN') or hasAuthority('teacher-application:read:*')";
    private static final String TEACHER_APPLICATION_MANAGE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('teacher-application:manage:*')";

    private final TeacherApplicationAdminViewService teacherApplicationAdminViewService;

    @PreAuthorize(TEACHER_APPLICATION_READ_ACCESS)
    @GetMapping
    public String teacherApplications(
        @RequestParam(required = false) TeacherApplicationStatus status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        Model model,
        Authentication authentication
    ) {
        TeacherApplicationFilter filter = new TeacherApplicationFilter(status, keyword, page, size);

        model.addAttribute("active", "teacherApplications");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("pageModel", teacherApplicationAdminViewService.getTeacherApplications(filter));
        return "admin/request/teacher-application/teacher-applications";
    }

    @PreAuthorize(TEACHER_APPLICATION_READ_ACCESS)
    @GetMapping("/{applicationId}")
    public String teacherApplicationDetail(
        @PathVariable Long applicationId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "teacherApplications");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("detail", teacherApplicationAdminViewService.getTeacherApplication(applicationId));
        return "admin/request/teacher-application/teacher-application-detail";
    }

    @PreAuthorize(TEACHER_APPLICATION_MANAGE_ACCESS)
    @PostMapping("/{applicationId}/approve")
    public String approve(
        @PathVariable Long applicationId,
        @Valid @ModelAttribute ApproveTeacherApplicationRequest request,
        BindingResult bindingResult,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", getValidationMessage(bindingResult));
            return "redirect:/admin/request/teacher-applications/" + applicationId;
        }

        teacherApplicationAdminViewService.approve(
            userDetails.getUserId(),
            applicationId,
            request.classroomId(),
            request.teacherStartAt(),
            request.teacherEndAt(),
            request.note()
        );
        redirectAttributes.addFlashAttribute("message", "교원 신청을 승인했습니다.");
        return "redirect:/admin/request/teacher-applications/" + applicationId;
    }

    @PreAuthorize(TEACHER_APPLICATION_MANAGE_ACCESS)
    @PostMapping("/{applicationId}/reject")
    public String reject(
        @PathVariable Long applicationId,
        @Valid @ModelAttribute RejectTeacherApplicationRequest request,
        BindingResult bindingResult,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", getValidationMessage(bindingResult));
            return "redirect:/admin/request/teacher-applications/" + applicationId;
        }

        teacherApplicationAdminViewService.reject(userDetails.getUserId(), applicationId, request.note());
        redirectAttributes.addFlashAttribute("message", "교원 신청을 반려했습니다.");
        return "redirect:/admin/request/teacher-applications/" + applicationId;
    }

    private String getValidationMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors()
            .stream()
            .findFirst()
            .map(this::getErrorMessage)
            .orElse("입력값이 올바르지 않습니다.");
    }

    private String getErrorMessage(ObjectError error) {
        if (error instanceof FieldError fieldError && fieldError.getDefaultMessage() != null) {
            return fieldError.getDefaultMessage();
        }
        if (error.getDefaultMessage() != null) {
            return error.getDefaultMessage();
        }
        return "입력값이 올바르지 않습니다.";
    }
}
