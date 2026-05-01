package geumjeongyahak.e2e.department;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.department.v1.dto.request.CreateDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentSimpleResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: Department 생성 테스트")
class DepartmentCreateTest extends DepartmentBaseTest {

    @Test
    @DisplayName("관리자 권한으로 Department 생성 성공(201 Created)")
    void createDepartment_Success() {
        String uniqueName = "개발팀_" + System.currentTimeMillis();
        CreateDepartmentRequest req = new CreateDepartmentRequest(
                uniqueName,
                "소프트웨어 개발을 담당하는 팀",
                null
        );

        var res = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo(uniqueName))
            .body("description", equalTo("소프트웨어 개발을 담당하는 팀"))
            .log().all()
            .extract()
            .as(DepartmentSimpleResponse.class);

        departmentTestHelper.setDepartment(res.id());
    }

    @Test
    @DisplayName("여러 Department 생성 성공(201 Created)")
    void createMultipleDepartments_Success() {
        String[] departments = {"재정팀", "학사팀", "홍보팀"};

        for (String dept : departments) {
            String uniqueName = dept + "_" + System.currentTimeMillis();
            CreateDepartmentRequest req = new CreateDepartmentRequest(
                    uniqueName,
                    uniqueName + " 설명",
                    null
            );

            var res = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .post()
            .then()
                .statusCode(201)
                .body("name", equalTo(uniqueName))
                .log().all()
                .extract()
                .as(DepartmentSimpleResponse.class);

            departmentTestHelper.setDepartment(res.id());
        }
    }

    @Test
    @DisplayName("일반 사용자 권한으로 Department 생성 실패(403 Forbidden)")
    void createDepartment_Forbidden() {
        String uniqueName = "금지된팀_" + System.currentTimeMillis();
        CreateDepartmentRequest req = new CreateDepartmentRequest(
                uniqueName,
                "생성 금지된 팀",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Department 생성 실패(401 Unauthorized)")
    void createDepartment_Unauthorized() {
        String uniqueName = "인증없음팀_" + System.currentTimeMillis();
        CreateDepartmentRequest req = new CreateDepartmentRequest(
                uniqueName,
                "인증 없는 요청",
                null
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("필수 필드 누락 시 Department 생성 실패(400 Bad Request)")
    void createDepartment_MissingRequiredFields() {
        String invalidReq = """
            {
                "description": "설명만 있음"
            }
            """;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(invalidReq)
        .when()
            .post()
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))
            .log().all();
    }

    @Test
    @DisplayName("빈 이름으로 Department 생성 실패(400 Bad Request)")
    void createDepartment_BlankName() {
        CreateDepartmentRequest req = new CreateDepartmentRequest(
                "",
                "빈 이름",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))
            .log().all();
    }

    @Test
    @DisplayName("빈 설명으로 Department 생성 실패(400 Bad Request)")
    void createDepartment_BlankDescription() {
        String uniqueName = "빈설명팀_" + System.currentTimeMillis();
        CreateDepartmentRequest req = new CreateDepartmentRequest(
                uniqueName,
                "",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))
            .log().all();
    }
}
