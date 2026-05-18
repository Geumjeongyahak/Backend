package geumjeongyahak.domain.request.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.service.AbsenceRequestAdminViewService;
import geumjeongyahak.domain.request.service.AbsenceRequestAdminViewService.AbsenceRequestFilter;
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
@RequestMapping("/admin/request/absence/absence-requests")
@RequiredArgsConstructor
public class AbsenceRequestViewController {

    private static final String ABSENCE_REQUEST_READ_ACCESS =
        "hasRole('ADMIN') or hasAuthority('absence-request:read:*')";
    private static final String ABSENCE_REQUEST_MANAGE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('absence-request:manage:*')";

    private final AbsenceRequestAdminViewService absenceRequestAdminViewService;

    @PreAuthorize(ABSENCE_REQUEST_READ_ACCESS)
    @GetMapping
    public String absenceRequests(
        @RequestParam(required = false) RequestStatus status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication
    ) {
        AbsenceRequestFilter filter = new AbsenceRequestFilter(status, keyword, page, size);
        model.addAttribute("active", "absenceRequests");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", absenceRequestAdminViewService.getStatuses());
        model.addAttribute("absenceRequestsPage", absenceRequestAdminViewService.getAbsenceRequests(userDetails.getUserId(), filter));
        model.addAttribute("absenceRequestAdminViewService", absenceRequestAdminViewService);
        return "admin/request/absence/absence-requests";
    }

    @PreAuthorize(ABSENCE_REQUEST_MANAGE_ACCESS)
    @PostMapping("/{requestId}/approve")
    public String approve(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        absenceRequestAdminViewService.approve(userDetails.getUserId(), requestId);
        redirectAttributes.addFlashAttribute("message", "결석 요청을 승인했습니다.");
        return "redirect:/admin/request/absence/absence-requests";
    }

    @PreAuthorize(ABSENCE_REQUEST_MANAGE_ACCESS)
    @PostMapping("/{requestId}/reject")
    public String reject(
        @PathVariable Long requestId,
        @RequestParam String note,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        absenceRequestAdminViewService.reject(userDetails.getUserId(), requestId, note);
        redirectAttributes.addFlashAttribute("message", "결석 요청을 반려했습니다.");
        return "redirect:/admin/request/absence/absence-requests";
    }
}
