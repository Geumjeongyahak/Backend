package geumjeongyahak.domain.vendor.v1.controller;

import java.util.List;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.vendor.service.VendorService;
import geumjeongyahak.domain.vendor.v1.dto.request.ChargeVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.request.CreateVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.request.UpdateVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorBalanceHistoryResponse;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendor Admin", description = "거래처 관리 API")
public class VendorAdminController {

    private final VendorService vendorService;

    @Operation(summary = "거래처 목록 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<VendorResponse>> getVendors(@RequestParam(required = false) String keyword) {
        log.debug("GET /api/v1/admin/vendors (keyword={})", keyword);
        return ResponseEntity.ok(vendorService.getVendors(keyword));
    }

    @Operation(summary = "거래처 생성")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('vendor:manage:*')")
    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        log.debug("POST /api/v1/admin/vendors");
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.createVendor(request));
    }

    @Operation(summary = "거래처 상세 조회")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('vendor:read:*')")
    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> getVendor(@PathVariable Long vendorId) {
        log.debug("GET /api/v1/admin/vendors/{}", vendorId);
        return ResponseEntity.ok(vendorService.getVendor(vendorId));
    }

    @Operation(summary = "거래처 수정")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('vendor:manage:*')")
    @PatchMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> updateVendor(
        @PathVariable Long vendorId,
        @RequestBody UpdateVendorRequest request
    ) {
        log.debug("PATCH /api/v1/admin/vendors/{}", vendorId);
        return ResponseEntity.ok(vendorService.updateVendor(vendorId, request));
    }

    @Operation(summary = "거래처 삭제")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('vendor:manage:*')")
    @DeleteMapping("/{vendorId}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long vendorId) {
        log.debug("DELETE /api/v1/admin/vendors/{}", vendorId);
        vendorService.deleteVendor(vendorId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "거래처 금액 충전")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('vendor:manage:*')")
    @PostMapping("/{vendorId}/charges")
    public ResponseEntity<VendorResponse> chargeVendor(
        @PathVariable Long vendorId,
        @Valid @RequestBody ChargeVendorRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/admin/vendors/{}/charges", vendorId);
        return ResponseEntity.ok(vendorService.chargeVendor(userDetails.getUserId(), vendorId, request));
    }

    @Operation(summary = "거래처 잔액 이력 조회")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('vendor:read:*')")
    @GetMapping("/{vendorId}/histories")
    public ResponseEntity<List<VendorBalanceHistoryResponse>> getHistories(@PathVariable Long vendorId) {
        log.debug("GET /api/v1/admin/vendors/{}/histories", vendorId);
        return ResponseEntity.ok(vendorService.getHistories(vendorId));
    }
}
