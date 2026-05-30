package geumjeongyahak.domain.sitecontent.v1.controller;

import geumjeongyahak.domain.sitecontent.v1.dto.request.CreateSiteContentClassRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.CreateSiteContentDepartmentRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.CreateSiteHistoryRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.UpdateSiteHistoryRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.UpdateSiteContentClassRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.UpdateSiteContentDepartmentRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentClassResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentDepartmentResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteHistoryResponse;
import geumjeongyahak.domain.sitecontent.service.SiteHistoryService;
import geumjeongyahak.domain.sitecontent.service.SiteContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/site-contents")
@Tag(name = "Site Content Admin", description = "공개 사이트 콘텐츠 관리자 API")
public class SiteContentAdminController {

    private final SiteContentService siteContentService;
    private final SiteHistoryService siteHistoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "연혁 생성", description = "연혁 항목을 생성합니다.")
    @PostMapping("/history")
    public ResponseEntity<SiteHistoryResponse> createHistory(
        @Valid @RequestBody CreateSiteHistoryRequest request
    ) {
        log.debug("POST /api/v1/site-contents/history - 연혁 생성 요청");
        SiteHistoryResponse response = siteHistoryService.createHistory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "연혁 수정", description = "연혁 항목을 수정합니다.")
    @PutMapping("/history/{historyId}")
    public ResponseEntity<SiteHistoryResponse> updateHistory(
        @Parameter(description = "연혁 ID", example = "1")
        @PathVariable Long historyId,
        @Valid @RequestBody UpdateSiteHistoryRequest request
    ) {
        log.debug("PUT /api/v1/site-contents/history/{} - 연혁 수정 요청", historyId);
        return ResponseEntity.ok(siteHistoryService.updateHistory(historyId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "연혁 삭제", description = "연혁 항목을 삭제합니다.")
    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<Void> deleteHistory(
        @Parameter(description = "연혁 ID", example = "1")
        @PathVariable Long historyId
    ) {
        log.debug("DELETE /api/v1/site-contents/history/{} - 연혁 삭제 요청", historyId);
        siteHistoryService.deleteHistory(historyId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "기관 부서 정보 생성", description = "교장 또는 부서 소개 항목을 생성합니다.")
    @PostMapping("/departments")
    public ResponseEntity<SiteContentDepartmentResponse> createDepartmentInfo(
        @Valid @RequestBody CreateSiteContentDepartmentRequest request
    ) {
        log.debug("POST /api/v1/site-contents/departments - 기관 부서 정보 생성 요청");
        SiteContentDepartmentResponse response = siteContentService.createDepartmentPageContent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "기관 부서 정보 수정", description = "교장 또는 부서 소개 항목을 수정합니다.")
    @PutMapping("/departments/{departmentInfoId}")
    public ResponseEntity<SiteContentDepartmentResponse> updateDepartmentInfo(
        @Parameter(description = "기관 부서 정보 ID", example = "1")
        @PathVariable Long departmentInfoId,
        @Valid @RequestBody UpdateSiteContentDepartmentRequest request
    ) {
        log.debug("PUT /api/v1/site-contents/departments/{} - 기관 부서 정보 수정 요청", departmentInfoId);
        return ResponseEntity.ok(siteContentService.updateDepartmentPageContent(departmentInfoId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "기관 부서 정보 삭제", description = "교장 또는 부서 소개 항목을 삭제합니다.")
    @DeleteMapping("/departments/{departmentInfoId}")
    public ResponseEntity<Void> deleteDepartmentInfo(
        @Parameter(description = "기관 부서 정보 ID", example = "1")
        @PathVariable Long departmentInfoId
    ) {
        log.debug("DELETE /api/v1/site-contents/departments/{} - 기관 부서 정보 삭제 요청", departmentInfoId);
        siteContentService.deleteDepartmentPageContent(departmentInfoId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "기관 반 정보 생성", description = "반 소개 항목을 생성합니다.")
    @PostMapping("/classes")
    public ResponseEntity<SiteContentClassResponse> createClassInfo(
        @Valid @RequestBody CreateSiteContentClassRequest request
    ) {
        log.debug("POST /api/v1/site-contents/classes - 기관 반 정보 생성 요청");
        SiteContentClassResponse response = siteContentService.createClassPageContent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "기관 반 정보 수정", description = "반 소개 항목을 수정합니다.")
    @PutMapping("/classes/{classInfoId}")
    public ResponseEntity<SiteContentClassResponse> updateClassInfo(
        @Parameter(description = "기관 반 정보 ID", example = "1")
        @PathVariable Long classInfoId,
        @Valid @RequestBody UpdateSiteContentClassRequest request
    ) {
        log.debug("PUT /api/v1/site-contents/classes/{} - 기관 반 정보 수정 요청", classInfoId);
        return ResponseEntity.ok(siteContentService.updateClassPageContent(classInfoId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "기관 반 정보 삭제", description = "반 소개 항목을 삭제합니다.")
    @DeleteMapping("/classes/{classInfoId}")
    public ResponseEntity<Void> deleteClassInfo(
        @Parameter(description = "기관 반 정보 ID", example = "1")
        @PathVariable Long classInfoId
    ) {
        log.debug("DELETE /api/v1/site-contents/classes/{} - 기관 반 정보 삭제 요청", classInfoId);
        siteContentService.deleteClassPageContent(classInfoId);
        return ResponseEntity.noContent().build();
    }
}
