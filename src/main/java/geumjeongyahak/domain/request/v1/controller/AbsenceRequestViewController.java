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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/request/absence/absence-requests")
@RequiredArgsConstructor
public class AbsenceRequestViewController {

    private static final String ABSENCE_REQUEST_READ_ACCESS =
        "hasRole('ADMIN') or hasAuthority('absence-request:read:*')";

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
}
