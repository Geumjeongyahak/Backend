package geumjeongyahak.e2e.sitecontent;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.sitecontent.entity.SiteContent;
import geumjeongyahak.domain.sitecontent.enums.SiteContentType;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: SiteContent 관리자 CRUD 테스트")
class SiteContentAdminTest extends SiteContentBaseTest {

    @Test
    @DisplayName("관리자가 연혁 정보를 생성, 수정, 삭제할 수 있다")
    void historyCrud_Admin_Success() {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "금정열린배움터 시작");
        createRequest.put("historyDate", "1997-01-01");
        createRequest.put("detail", "첫 수업을 시작했습니다.");
        createRequest.put("links", List.of(
            Map.of("label", "소개", "href", "https://example.com/history"),
            Map.of("label", "사진첩", "href", "https://example.com/photos")
        ));
        createRequest.put("photos", List.of(Map.of(
            "src", "https://example.com/photo.jpg",
            "alt", "첫 수업 사진"
        )));

        Integer id = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201)
            .body("title", equalTo("금정열린배움터 시작"))
            .body("historyDate", equalTo("1997-01-01"))
            .body("links", hasSize(2))
            .body("links[1].label", equalTo("사진첩"))
            .body("photos", hasSize(1))
            .extract()
            .path("id");

        Map<String, Object> createResponse = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .extract()
            .path("history[0]");
        org.assertj.core.api.Assertions.assertThat(createResponse)
            .doesNotContainKeys("linkLabel", "linkHref");

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "금정열린배움터 확장");
        updateRequest.put("historyDate", "1998-02-01");
        updateRequest.put("detail", null);
        updateRequest.put("links", List.of());
        updateRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(updateRequest)
        .when()
            .put("/history/{id}", id)
        .then()
            .statusCode(200)
            .body("title", equalTo("금정열린배움터 확장"))
            .body("historyDate", equalTo("1998-02-01"))
            .body("detail", equalTo(null))
            .body("links", hasSize(0))
            .body("photos", hasSize(0));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/history/{id}", id)
        .then()
            .statusCode(204);

        given()
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("history", hasSize(0));
    }

    @Test
    @DisplayName("연혁 생성 날짜에 따라 기존 연혁 사이에 조회된다")
    void createHistory_WithMiddleDate_SortedByHistoryDate() {
        Map<String, Object> latestRequest = new HashMap<>();
        latestRequest.put("title", "세 번째");
        latestRequest.put("historyDate", "2001-01-01");
        latestRequest.put("photos", List.of());

        Map<String, Object> earliestRequest = new HashMap<>();
        earliestRequest.put("title", "첫 번째");
        earliestRequest.put("historyDate", "1997-01-01");
        earliestRequest.put("photos", List.of());

        Map<String, Object> middleRequest = new HashMap<>();
        middleRequest.put("title", "두 번째");
        middleRequest.put("historyDate", "1999-01-01");
        middleRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(latestRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(earliestRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(middleRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201);

        given()
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("history", hasSize(3))
            .body("history[0].title", equalTo("첫 번째"))
            .body("history[1].title", equalTo("두 번째"))
            .body("history[2].title", equalTo("세 번째"));
    }

    @Test
    @DisplayName("연혁 수정 시 사진 목록은 요청값 기준으로 전체 교체된다")
    void updateHistory_ReplacesPhotos_Success() {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "금정열린배움터 시작");
        createRequest.put("historyDate", "1997-01-01");
        createRequest.put("photos", List.of(
            Map.of("src", "https://example.com/old-1.jpg", "alt", "기존 사진 1"),
            Map.of("src", "https://example.com/old-2.jpg", "alt", "기존 사진 2")
        ));

        Integer id = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201)
            .body("photos", hasSize(2))
            .extract()
            .path("id");

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "금정열린배움터 시작");
        updateRequest.put("historyDate", "1997-01-01");
        updateRequest.put("photos", List.of(Map.of(
            "src", "https://example.com/new.jpg",
            "alt", "새 사진"
        )));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(updateRequest)
        .when()
            .put("/history/{id}", id)
        .then()
            .statusCode(200)
            .body("photos", hasSize(1))
            .body("photos[0].src", equalTo("https://example.com/new.jpg"))
            .body("photos[0].alt", equalTo("새 사진"));
    }

    @Test
    @DisplayName("연혁 수정에서 제외된 사진 파일은 soft delete 된다")
    void updateHistory_RemovedPhotoFile_SoftDeleted() {
        File file = saveSiteContentImageFile();
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "금정열린배움터 시작");
        createRequest.put("historyDate", "1997-01-01");
        createRequest.put("photos", List.of(Map.of(
            "fileId", file.getId().toString(),
            "src", file.getPublicUrl(),
            "alt", "기존 사진"
        )));

        Integer id = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "금정열린배움터 시작");
        updateRequest.put("historyDate", "1997-01-01");
        updateRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(updateRequest)
        .when()
            .put("/history/{id}", id)
        .then()
            .statusCode(200)
            .body("photos", hasSize(0));

        org.assertj.core.api.Assertions.assertThat(fileRepository.findById(file.getId()).orElseThrow().isDeleted())
            .isTrue();
    }

    @Test
    @DisplayName("연혁 삭제 시 연결된 사진 파일은 soft delete 된다")
    void deleteHistory_LinkedPhotoFile_SoftDeleted() {
        File file = saveSiteContentImageFile();
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "금정열린배움터 시작");
        createRequest.put("historyDate", "1997-01-01");
        createRequest.put("photos", List.of(Map.of(
            "fileId", file.getId().toString(),
            "src", file.getPublicUrl(),
            "alt", "기존 사진"
        )));

        Integer id = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/history/{id}", id)
        .then()
            .statusCode(204);

        org.assertj.core.api.Assertions.assertThat(fileRepository.findById(file.getId()).orElseThrow().isDeleted())
            .isTrue();
    }

    @Test
    @DisplayName("연혁 생성 요청은 title과 photos.src를 검증한다")
    void createHistory_InvalidRequest_BadRequest() {
        Map<String, Object> blankTitleRequest = new HashMap<>();
        blankTitleRequest.put("title", " ");
        blankTitleRequest.put("historyDate", "1997-01-01");
        blankTitleRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(blankTitleRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(400);

        Map<String, Object> blankPhotoSrcRequest = new HashMap<>();
        blankPhotoSrcRequest.put("title", "금정열린배움터 시작");
        blankPhotoSrcRequest.put("historyDate", "1997-01-01");
        blankPhotoSrcRequest.put("photos", List.of(Map.of(
            "src", " ",
            "alt", "빈 URL"
        )));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(blankPhotoSrcRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(400);

        Map<String, Object> blankLinkRequest = new HashMap<>();
        blankLinkRequest.put("title", "금정열린배움터 시작");
        blankLinkRequest.put("historyDate", "1997-01-01");
        blankLinkRequest.put("links", List.of(Map.of(
            "label", " ",
            "href", "https://example.com/history"
        )));
        blankLinkRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(blankLinkRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(400);

        Map<String, Object> missingHistoryDateRequest = new HashMap<>();
        missingHistoryDateRequest.put("title", "금정열린배움터 시작");
        missingHistoryDateRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(missingHistoryDateRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(400);

        Map<String, Object> legacyLinkRequest = new HashMap<>();
        legacyLinkRequest.put("title", "금정열린배움터 시작");
        legacyLinkRequest.put("historyDate", "1997-01-01");
        legacyLinkRequest.put("linkLabel", "소개");
        legacyLinkRequest.put("linkHref", "https://example.com/history");
        legacyLinkRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(legacyLinkRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("연혁 수정 요청은 제거된 단일 링크 필드를 거부하고 기존 링크를 유지한다")
    void updateHistory_LegacyLinkFields_BadRequestAndKeepsLinks() {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "금정열린배움터 시작");
        createRequest.put("historyDate", "1997-01-01");
        createRequest.put("links", List.of(Map.of(
            "label", "소개",
            "href", "https://example.com/history"
        )));
        createRequest.put("photos", List.of());

        Integer id = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        Map<String, Object> legacyUpdateRequest = new HashMap<>();
        legacyUpdateRequest.put("title", "금정열린배움터 수정");
        legacyUpdateRequest.put("historyDate", "1997-01-01");
        legacyUpdateRequest.put("linkLabel", "변경 링크");
        legacyUpdateRequest.put("linkHref", "https://example.com/changed");
        legacyUpdateRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(legacyUpdateRequest)
        .when()
            .put("/history/{id}", id)
        .then()
            .statusCode(400);

        given()
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("history[0].title", equalTo("금정열린배움터 시작"))
            .body("history[0].links", hasSize(1))
            .body("history[0].links[0].label", equalTo("소개"))
            .body("history[0].links[0].href", equalTo("https://example.com/history"));
    }

    @Test
    @DisplayName("관리자 화면 연혁 링크 입력은 잘못된 형식을 저장하지 않는다")
    void createHistory_AdminViewInvalidLinks_BadRequest() {
        String adminSessionId = loginAdminSession(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);

        given()
            .basePath("")
            .cookie("JSESSIONID", adminSessionId)
            .contentType(ContentType.URLENC)
            .formParam("title", "금정열린배움터 시작")
            .formParam("historyDate", "1997-01-01")
            .formParam("linksText", "소개 https://example.com/history")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/site-content/history")
        .then()
            .statusCode(400);

        org.assertj.core.api.Assertions.assertThat(siteHistoryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("관리자 화면 연혁 링크 입력은 빈 URL과 긴 라벨을 저장하지 않는다")
    void createHistory_AdminViewBlankHrefAndLongLabel_BadRequest() {
        String adminSessionId = loginAdminSession(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
        String longLabel = "가".repeat(121);

        given()
            .basePath("")
            .cookie("JSESSIONID", adminSessionId)
            .contentType(ContentType.URLENC)
            .formParam("title", "금정열린배움터 시작")
            .formParam("historyDate", "1997-01-01")
            .formParam("linksText", "소개|")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/site-content/history")
        .then()
            .statusCode(400);

        given()
            .basePath("")
            .cookie("JSESSIONID", adminSessionId)
            .contentType(ContentType.URLENC)
            .formParam("title", "금정열린배움터 시작")
            .formParam("historyDate", "1997-01-01")
            .formParam("linksText", longLabel + "|https://example.com/history")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/site-content/history")
        .then()
            .statusCode(400);

        org.assertj.core.api.Assertions.assertThat(siteHistoryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("관리자가 기관 부서 정보를 생성, 수정, 삭제할 수 있다")
    void departmentInfoCrud_Admin_Success() {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "교육연구부");
        createRequest.put("name", null);
        createRequest.put("responsibilities", List.of("신입 선생님 면접"));

        Integer id = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/departments")
        .then()
            .statusCode(201)
            .body("title", equalTo("교육연구부"))
            .body("name", equalTo(null))
            .body("responsibilities", hasSize(1))
            .extract()
            .path("id");

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "교육연구부");
        updateRequest.put("name", "담당자");
        updateRequest.put("responsibilities", List.of("신입 선생님 면접", "연구 수업 관리"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(updateRequest)
        .when()
            .put("/departments/{id}", id)
        .then()
            .statusCode(200)
            .body("name", equalTo("담당자"))
            .body("responsibilities", hasSize(2));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/departments/{id}", id)
        .then()
            .statusCode(204);

        given()
        .when()
            .get("/departments")
        .then()
            .statusCode(200)
            .body("principal.title", equalTo("교장"))
            .body("principal.name", equalTo(""))
            .body("principal.responsibilities", hasSize(0))
            .body("departments", hasSize(0));
    }

    @Test
    @DisplayName("기존 교장 정보는 부서 API에서 같은 DTO로 수정할 수 있다")
    void updatePrincipalInfo_Admin_Success() {
        SiteContent principal =
            SiteContent.builder()
                .contentType(SiteContentType.PRINCIPAL)
                .title("교장")
                .name("정해웅")
                .build();
        siteContentRepository.save(principal);

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "교장");
        updateRequest.put("name", "홍길동");
        updateRequest.put("responsibilities", List.of("운영 총괄"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(updateRequest)
        .when()
            .put("/departments/{id}", principal.getId())
        .then()
            .statusCode(200)
            .body("title", equalTo("교장"))
            .body("name", equalTo("홍길동"))
            .body("responsibilities", hasSize(1));
    }

    @Test
    @DisplayName("교장 정보는 프론트 렌더링 계약 보호를 위해 REST API에서 삭제할 수 없다")
    void deletePrincipalInfo_Admin_BadRequest() {
        SiteContent principal = SiteContent.builder()
            .contentType(SiteContentType.PRINCIPAL)
            .title("교장")
            .name("정해웅")
            .build();
        siteContentRepository.save(principal);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/departments/{id}", principal.getId())
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("부서 정보 생성 요청은 title을 검증하고 프론트 필드만 받는다")
    void createDepartmentInfo_DefaultTypeAndValidation_Success() {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "교육연구부");
        createRequest.put("responsibilities", List.of("신입 선생님 면접"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/departments")
        .then()
            .statusCode(201)
            .body("title", equalTo("교육연구부"))
            .body("responsibilities", hasSize(1));

        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("title", "");
        invalidRequest.put("responsibilities", List.of("신입 선생님 면접"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(invalidRequest)
        .when()
            .post("/departments")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("관리자가 기관 반 정보를 생성, 수정, 삭제할 수 있다")
    void classInfoCrud_Admin_Success() {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("name", "벚꽃반");
        createRequest.put("groupId", "weekday");
        createRequest.put("description", List.of("평일 기초 학습반"));

        Integer id = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(createRequest)
        .when()
            .post("/classes")
        .then()
            .statusCode(201)
            .body("name", equalTo("벚꽃반"))
            .body("description", hasSize(1))
            .extract()
            .path("id");

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("name", "주말 오전반");
        updateRequest.put("groupId", "weekendMorning");
        updateRequest.put("description", List.of("주말 오전 운영", "기초 학습"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(updateRequest)
        .when()
            .put("/classes/{id}", id)
        .then()
            .statusCode(200)
            .body("name", equalTo("주말 오전반"))
            .body("description", hasSize(2));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/classes/{id}", id)
        .then()
            .statusCode(204);

        given()
        .when()
            .get("/classes")
        .then()
            .statusCode(200)
            .body("weekday", hasSize(0))
            .body("weekendMorning", hasSize(0))
            .body("weekendAfternoon", hasSize(0));
    }

    @Test
    @DisplayName("반 정보 생성 요청은 name과 groupId를 검증한다")
    void createClassInfo_InvalidRequest_BadRequest() {
        Map<String, Object> missingGroupRequest = new HashMap<>();
        missingGroupRequest.put("name", "벚꽃반");
        missingGroupRequest.put("description", List.of("평일 기초 학습반"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(missingGroupRequest)
        .when()
            .post("/classes")
        .then()
            .statusCode(400);

        Map<String, Object> blankNameRequest = new HashMap<>();
        blankNameRequest.put("name", " ");
        blankNameRequest.put("groupId", "weekday");
        blankNameRequest.put("description", List.of("평일 기초 학습반"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .body(blankNameRequest)
        .when()
            .post("/classes")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("관리자가 아니면 사이트 콘텐츠 쓰기가 거부된다")
    void writeSiteContent_NonAdmin_Forbidden() {
        Map<String, Object> departmentRequest = new HashMap<>();
        departmentRequest.put("title", "교육연구부");
        departmentRequest.put("responsibilities", List.of("신입 선생님 면접"));

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .body(departmentRequest)
        .when()
            .post("/departments")
        .then()
            .statusCode(403);

        Map<String, Object> historyRequest = new HashMap<>();
        historyRequest.put("title", "금정열린배움터 시작");
        historyRequest.put("historyDate", "1997-01-01");
        historyRequest.put("photos", List.of());

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .body(historyRequest)
        .when()
            .post("/history")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("매니저는 관리자 화면으로 사이트 콘텐츠를 수정할 수 없다")
    void adminViewWriteSiteContent_Manager_Forbidden() {
        String managerSessionId = loginAdminSession(TEST_MANAGER_USERNAME + "@test.com", "pw_" + TEST_MANAGER_USERNAME);

        given()
            .basePath("")
            .cookie("JSESSIONID", managerSessionId)
            .contentType(ContentType.URLENC)
            .formParam("contentType", "DEPARTMENT")
            .formParam("title", "교무기획부")
            .formParam("itemsText", "야학 행사 계획")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/site-content/contents")
        .then()
            .statusCode(anyOf(is(302), is(403)));

        org.assertj.core.api.Assertions.assertThat(siteContentRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("인증 없이 사이트 콘텐츠 쓰기가 거부된다")
    void writeSiteContent_Anonymous_Unauthorized() {
        Map<String, Object> departmentRequest = new HashMap<>();
        departmentRequest.put("title", "교육연구부");
        departmentRequest.put("responsibilities", List.of("신입 선생님 면접"));

        given()
            .contentType(ContentType.JSON)
            .body(departmentRequest)
        .when()
            .post("/departments")
        .then()
            .statusCode(401);

        Map<String, Object> classRequest = new HashMap<>();
        classRequest.put("name", "벚꽃반");
        classRequest.put("groupId", "weekday");
        classRequest.put("description", List.of("평일 기초 학습반"));

        given()
            .contentType(ContentType.JSON)
            .body(classRequest)
        .when()
            .post("/classes")
        .then()
            .statusCode(401);
    }

    private File saveSiteContentImageFile() {
        return fileRepository.save(File.builder()
            .storageKey("site-contents/" + UUID.randomUUID() + ".png")
            .bucket("test-bucket")
            .originalName("history.png")
            .contentType("image/png")
            .fileSize(10L)
            .ext("png")
            .publicUrl("https://example.com/history.png")
            .build());
    }

    private String loginAdminSession(String username, String password) {
        return given()
            .basePath("")
            .contentType(ContentType.URLENC)
            .formParam("username", username)
            .formParam("password", password)
            .redirects()
            .follow(false)
        .when()
            .post("/admin/auth/login")
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin"))
            .extract()
            .cookie("JSESSIONID");
    }
}
