package geumjeongyahak.domain.request.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/request/lesson-exchange")
@RequiredArgsConstructor
public class LessonExchangeRequestViewController {

    private static final String LESSON_EXCHANGE_MANAGE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('lesson-exchange-request:manage:*')";

    private final LessonExchangeRequestAdminViewService lessonExchangeRequestAdminViewService;

    @GetMapping
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("active", "lessonExchangeRequests");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("dashboard", lessonExchangeRequestAdminViewService.getDashboard());
        return "admin/request/lesson-exchange/dashboard";
    }

    @PreAuthorize(LESSON_EXCHANGE_MANAGE_ACCESS)
    @PostMapping("/{requestId}/approve")
    public String approve(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        lessonExchangeRequestAdminViewService.approve(userDetails.getUserId(), requestId);
        redirectAttributes.addFlashAttribute("message", "수업 교환 요청을 승인했습니다.");
        return "redirect:/admin/request/lesson-exchange";
    }

    @PreAuthorize(LESSON_EXCHANGE_MANAGE_ACCESS)
    @PostMapping("/{requestId}/reject")
    public String reject(
        @PathVariable Long requestId,
        @RequestParam String note,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        lessonExchangeRequestAdminViewService.reject(userDetails.getUserId(), requestId, note);
        redirectAttributes.addFlashAttribute("message", "수업 교환 요청을 반려했습니다.");
        return "redirect:/admin/request/lesson-exchange";
    }
}
