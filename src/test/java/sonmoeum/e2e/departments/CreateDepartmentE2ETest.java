package sonmoeum.e2e.departments;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("e2e")
@DisplayName("Department E2E Tests - Create")
class CreateDepartmentE2ETest extends BaseDepartmentE2ETest {

    @Test
    @DisplayName("부서 생성 성공")
    void createDepartmentSuccess() {
        String sessionCookie = getAdminSession();
        var departmentRequest = createDepartmentRequest("교육부", "교육 관련 업무를 담당하는 부서");

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(departmentRequest)
        .when()
            .post("/api/v1/departments")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.name", equalTo("교육부"))
            .body("data.description", equalTo("교육 관련 업무를 담당하는 부서"))
            .body("data.id", notNullValue());
    }

    @Test
    @DisplayName("부서 생성 실패 - 권한 없음")
    void createDepartmentFailUnauthorized() {
        var departmentRequest = createDepartmentRequest("교육부", "교육 관련 업무");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(departmentRequest)
        .when()
            .post("/api/v1/departments")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("부서 생성 실패 - 필수 필드 누락")
    void createDepartmentFailMissingFields() {
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("{}")
        .when()
            .post("/api/v1/departments")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
