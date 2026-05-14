package geumjeongyahak.domain.request.v1.controller;

import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/request/lesson-exchange")
@RequiredArgsConstructor
public class LessonExchangeRequestViewController {

    private final LessonExchangeRequestAdminViewService lessonExchangeRequestAdminViewService;

    @GetMapping
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("active", "lessonExchangeRequests");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("dashboard", lessonExchangeRequestAdminViewService.getDashboard());
        return "admin/request/lesson-exchange/dashboard";
    }
}
