package geumjeongyahak.e2e.sitecontent;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
        createRequest.put("detail", "첫 수업을 시작했습니다.");
        createRequest.put("linkLabel", "소개");
        createRequest.put("linkHref", "https://example.com/history");
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
            .body("photos", hasSize(1))
            .extract()
            .path("id");

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("title", "금정열린배움터 확장");
        updateRequest.put("detail", null);
        updateRequest.put("linkLabel", null);
        updateRequest.put("linkHref", null);
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
            .body("detail", equalTo(null))
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
    @DisplayName("연혁 수정 시 사진 목록은 요청값 기준으로 전체 교체된다")
    void updateHistory_ReplacesPhotos_Success() {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("title", "금정열린배움터 시작");
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
}
