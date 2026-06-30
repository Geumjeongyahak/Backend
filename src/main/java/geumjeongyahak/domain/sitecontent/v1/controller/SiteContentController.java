package geumjeongyahak.domain.sitecontent.v1.controller;

import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentClassesResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentDepartmentsResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteHistoriesResponse;
import geumjeongyahak.domain.sitecontent.service.SiteHistoryService;
import geumjeongyahak.domain.sitecontent.service.SiteContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/site-contents")
@Tag(name = "Site Content", description = "공개 사이트 콘텐츠 API")
public class SiteContentController {

    private final SiteContentService siteContentService;
    private final SiteHistoryService siteHistoryService;

    @Operation(summary = "연혁 정보 조회", description = "연혁 페이지 렌더링 정보를 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<SiteHistoriesResponse> getHistories() {
        log.debug("GET /api/v1/site-contents/history - 연혁 정보 조회 요청");
        return ResponseEntity.ok(siteHistoryService.getHistories());
    }

    @Operation(summary = "기관 부서 정보 조회", description = "교장 및 부서 소개 정보를 조회합니다.")
    @GetMapping("/departments")
    public ResponseEntity<SiteContentDepartmentsResponse> getDepartmentInfos() {
        log.debug("GET /api/v1/site-contents/departments - 기관 부서 정보 조회 요청");
        return ResponseEntity.ok(siteContentService.getDepartmentPageContents());
    }

    @Operation(summary = "기관 반 정보 조회", description = "주중반, 주말 오전반, 주말 오후반 소개 정보를 조회합니다.")
    @GetMapping("/classes")
    public ResponseEntity<SiteContentClassesResponse> getClassInfos() {
        log.debug("GET /api/v1/site-contents/classes - 기관 반 정보 조회 요청");
        return ResponseEntity.ok(siteContentService.getClassPageContents());
    }
}
