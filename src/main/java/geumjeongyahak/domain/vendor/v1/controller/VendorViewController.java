package geumjeongyahak.domain.vendor.v1.controller;

import java.util.UUID;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.vendor.service.VendorAdminViewService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestAdminViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/request/purchase/vendors")
@RequiredArgsConstructor
public class VendorViewController {

    private final VendorAdminViewService vendorAdminViewService;
    private final PurchaseRequestAdminViewService purchaseRequestAdminViewService;

    @GetMapping
    public String vendors(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "false") boolean activeOnly,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "vendors");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeOnly", activeOnly);
        model.addAttribute("sort", sort);
        model.addAttribute("vendors", vendorAdminViewService.getVendors(keyword, activeOnly, sort));
        return "admin/request/purchase/vendors";
    }

    @GetMapping("/new")
    public String createForm(Model model, Authentication authentication) {
        model.addAttribute("active", "vendors");
        model.addAttribute("adminName", authentication.getName());
        return "admin/request/purchase/vendors-form";
    }

    @PostMapping
    public String create(
        @RequestParam String name,
        @RequestParam(required = false) String description,
        RedirectAttributes redirectAttributes
    ) {
        Long vendorId = vendorAdminViewService.createVendor(name, description);
        redirectAttributes.addFlashAttribute("message", "거래처가 등록되었습니다.");
        return "redirect:/admin/request/purchase/vendors/" + vendorId;
    }

    @GetMapping("/{vendorId}")
    public String detail(
        @PathVariable Long vendorId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "vendors");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("vendor", vendorAdminViewService.getVendor(vendorId));
        model.addAttribute("histories", vendorAdminViewService.getHistories(vendorId));
        return "admin/request/purchase/vendors-detail";
    }

    @PostMapping("/{vendorId}")
    public String update(
        @PathVariable Long vendorId,
        @RequestParam String name,
        @RequestParam(required = false) String description,
        @RequestParam(defaultValue = "false") boolean isActive,
        RedirectAttributes redirectAttributes
    ) {
        vendorAdminViewService.updateVendor(vendorId, name, description, isActive);
        redirectAttributes.addFlashAttribute("message", "거래처가 수정되었습니다.");
        return "redirect:/admin/request/purchase/vendors/" + vendorId;
    }

    @PostMapping("/{vendorId}/charges")
    public String charge(
        @PathVariable Long vendorId,
        @RequestParam Long amount,
        @RequestParam(required = false) String memo,
        @RequestParam(required = false) UUID receiptFileId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        vendorAdminViewService.chargeVendor(userDetails.getUserId(), vendorId, amount, memo, receiptFileId);
        redirectAttributes.addFlashAttribute("message", "거래처 금액이 충전되었습니다.");
        return "redirect:/admin/request/purchase/vendors/" + vendorId;
    }

    @PostMapping("/{vendorId}/delete")
    public String delete(
        @PathVariable Long vendorId,
        RedirectAttributes redirectAttributes
    ) {
        vendorAdminViewService.deleteVendor(vendorId);
        redirectAttributes.addFlashAttribute("message", "거래처가 삭제되었습니다.");
        return "redirect:/admin/request/purchase/vendors";
    }

    @ResponseBody
    @PostMapping("/receipt-images")
    public ResponseEntity<FileUploadResponse> uploadReceiptImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(purchaseRequestAdminViewService.uploadReceiptImage(file));
    }
}
