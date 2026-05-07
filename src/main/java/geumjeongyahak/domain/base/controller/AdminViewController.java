package geumjeongyahak.domain.base.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import geumjeongyahak.domain.base.service.AdminDashboardService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/auth/login")
    public String login() {
        return "admin/auth/login";
    }

    @GetMapping
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("active", "dashboard");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("summary", adminDashboardService.getSummary());
        return "admin/dashboard";
    }

    @GetMapping("favicon.ico")
    public String favicon() {
        return "forward:/static/favicon.ico";
    }
}
