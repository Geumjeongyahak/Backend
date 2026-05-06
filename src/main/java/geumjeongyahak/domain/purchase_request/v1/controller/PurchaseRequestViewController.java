package geumjeongyahak.domain.purchase_request.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestAdminViewService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestAdminViewService.PurchaseRequestFilter;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/request/purchase/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestViewController {

    private final PurchaseRequestAdminViewService purchaseRequestAdminViewService;

    @GetMapping
    public String purchaseRequests(
        @RequestParam(required = false) PurchaseRequestStatus status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String classroomName,
        @RequestParam(required = false) String requestedByName,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        PurchaseRequestFilter filter = new PurchaseRequestFilter(status, keyword, classroomName, requestedByName, page, size, sort);
        model.addAttribute("active", "purchaseRequests");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", purchaseRequestAdminViewService.getStatuses());
        model.addAttribute("purchaseRequestsPage", purchaseRequestAdminViewService.getPurchaseRequests(filter));
        return "admin/request/purchase/purchase-requests";
    }

    @GetMapping("/{requestId}")
    public String purchaseRequestDetail(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "purchaseRequests");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("request", purchaseRequestAdminViewService.getPurchaseRequest(userDetails.getUserId(), requestId));
        return "admin/request/purchase/purchase-requests-detail";
    }

    @PostMapping("/{requestId}/approve")
    public String approve(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        purchaseRequestAdminViewService.approve(userDetails.getUserId(), requestId);
        redirectAttributes.addFlashAttribute("message", "구매 요청을 승인했습니다.");
        return "redirect:/admin/request/purchase/purchase-requests/" + requestId;
    }

    @PostMapping("/{requestId}/reject")
    public String reject(
        @PathVariable Long requestId,
        @RequestParam String note,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        purchaseRequestAdminViewService.reject(userDetails.getUserId(), requestId, note);
        redirectAttributes.addFlashAttribute("message", "구매 요청을 반려했습니다.");
        return "redirect:/admin/request/purchase/purchase-requests/" + requestId;
    }
}
