package geumjeongyahak.domain.purchase_request.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestAdminViewService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestAdminViewService.PurchaseRequestFilter;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.PurchaseRequestForm;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
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

    @GetMapping("/new")
    public String createForm(
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "purchaseRequests");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("classrooms", purchaseRequestAdminViewService.getAllClassrooms());
        model.addAttribute("vendors", purchaseRequestAdminViewService.getAllVendors());

        PurchaseRequestForm form = new PurchaseRequestForm();
        form.getItems().add(new PurchaseRequestForm.ItemForm());
        model.addAttribute("form", form);

        return "admin/request/purchase/purchase-requests-form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") PurchaseRequestForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("active", "purchaseRequests");
            model.addAttribute("adminName", authentication.getName());
            model.addAttribute("classrooms", purchaseRequestAdminViewService.getAllClassrooms());
            model.addAttribute("vendors", purchaseRequestAdminViewService.getAllVendors());
            return "admin/request/purchase/purchase-requests-form";
        }

        List<CreatePurchaseRequestRequest.Item> items = form.getItems().stream()
            .map(i -> new CreatePurchaseRequestRequest.Item(i.getName(), i.getReason(), i.getPrice(), i.getReceiptFileId()))
            .toList();

        Long requestId = purchaseRequestAdminViewService.createPurchaseRequest(
            userDetails.getUserId(),
            form.getClassroomId(),
            form.getTitle(),
            form.getContent(),
            form.getPaymentMethod(),
            form.getVendorId(),
            items);

        redirectAttributes.addFlashAttribute("message", "구매 요청이 생성되었습니다.");
        return "redirect:/admin/request/purchase/purchase-requests/" + requestId;
    }

    @GetMapping("/{requestId}/edit")
    public String editForm(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication
    ) {
        PurchaseRequestDetailResponse response = purchaseRequestAdminViewService.getPurchaseRequest(userDetails.getUserId(), requestId);

        PurchaseRequestForm form = new PurchaseRequestForm();
        form.setClassroomId(response.classroomId());
        form.setTitle(response.title());
        form.setContent(response.content());
        form.setPaymentMethod(response.paymentMethod());
        form.setVendorId(response.vendorId());
        form.setItems(response.items().stream()
            .map(i -> {
                PurchaseRequestForm.ItemForm item = new PurchaseRequestForm.ItemForm();
                item.setId(i.id());
                item.setName(i.name());
                item.setReason(i.reason());
                item.setPrice(i.price());
                item.setReceiptFileId(i.receiptFileId());
                item.setReceiptFileUrl(i.receiptFileUrl());
                return item;
            })
            .collect(java.util.stream.Collectors.toList()));

        model.addAttribute("active", "purchaseRequests");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("request", response);
        model.addAttribute("form", form);
        model.addAttribute("classrooms", purchaseRequestAdminViewService.getAllClassrooms());
        model.addAttribute("vendors", purchaseRequestAdminViewService.getAllVendors());
        return "admin/request/purchase/purchase-requests-edit";
    }

    @PostMapping("/{requestId}")
    public String update(
        @PathVariable Long requestId,
        @Valid @ModelAttribute("form") PurchaseRequestForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("active", "purchaseRequests");
            model.addAttribute("adminName", authentication.getName());
            model.addAttribute("classrooms", purchaseRequestAdminViewService.getAllClassrooms());
            model.addAttribute("vendors", purchaseRequestAdminViewService.getAllVendors());
            model.addAttribute("request", purchaseRequestAdminViewService.getPurchaseRequest(userDetails.getUserId(), requestId));
            return "admin/request/purchase/purchase-requests-edit";
        }

        List<CreatePurchaseRequestRequest.Item> items = form.getItems().stream()
            .map(i -> new CreatePurchaseRequestRequest.Item(i.getName(), i.getReason(), i.getPrice(), i.getReceiptFileId()))
            .toList();

        purchaseRequestAdminViewService.updatePurchaseRequest(
            userDetails.getUserId(),
            requestId,
            form.getTitle(),
            form.getContent(),
            form.getPaymentMethod(),
            form.getVendorId(),
            items);

        redirectAttributes.addFlashAttribute("message", "구매 요청이 수정되었습니다.");
        return "redirect:/admin/request/purchase/purchase-requests/" + requestId;
    }

    @PostMapping("/{requestId}/delete")
    public String delete(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        purchaseRequestAdminViewService.deletePurchaseRequest(userDetails.getUserId(), requestId);
        redirectAttributes.addFlashAttribute("message", "구매 요청이 삭제되었습니다.");
        return "redirect:/admin/request/purchase/purchase-requests";
    }

    @PostMapping("/{requestId}/report")
    public String report(
        @PathVariable Long requestId,
        @Valid @ModelAttribute("form") PurchaseRequestForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "입력값이 올바르지 않습니다.");
            return "redirect:/admin/request/purchase/purchase-requests/" + requestId + "/edit";
        }

        List<geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest.ItemReport> itemReports = form.getItems().stream()
            .filter(i -> i.getId() != null)
            .map(i -> new geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest.ItemReport(i.getId(), i.getReceiptFileId()))
            .toList();

        purchaseRequestAdminViewService.report(userDetails.getUserId(), requestId, itemReports);

        redirectAttributes.addFlashAttribute("message", "구매 보고가 완료되었습니다.");
        return "redirect:/admin/request/purchase/purchase-requests/" + requestId;
    }

    @PostMapping("/{requestId}/approve")
    public String approve(
        @PathVariable Long requestId,
        @RequestParam String note,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        purchaseRequestAdminViewService.approve(userDetails.getUserId(), requestId, note);
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

    @PostMapping("/{requestId}/confirm")
    public String confirm(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        purchaseRequestAdminViewService.confirm(userDetails.getUserId(), requestId);
        redirectAttributes.addFlashAttribute("message", "구매 결재 확인을 완료했습니다.");
        return "redirect:/admin/request/purchase/purchase-requests/" + requestId;
    }

    @ResponseBody
    @PostMapping("/receipt-images")
    public ResponseEntity<FileUploadResponse> uploadReceiptImage(
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(purchaseRequestAdminViewService.uploadReceiptImage(file));
    }
}
