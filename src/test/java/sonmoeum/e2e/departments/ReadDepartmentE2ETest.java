package sonmoeum.e2e.departments;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import sonmoeum.domain.department.entity.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("e2e")
@DisplayName("Department E2E Tests - Read")
class ReadDepartmentE2ETest extends BaseDepartmentE2ETest {

    @Test
    @DisplayName("부서 상세 조회 성공")
    void getDepartmentByIdSuccess() {
        Department department = departmentRepository.save(new Department(
                "교육부",
                null,
                "교육 관련 업무"));
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/departments/" + department.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(department.getId().intValue()))
            .body("data.name", equalTo("교육부"));
    }

    @Test
    @DisplayName("부서 상세 조회 실패 - 존재하지 않는 ID")
    void getDepartmentByIdFailNotFound() {
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/departments/99999")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("부서 목록 조회 성공")
    void getDepartmentsSuccess() {
        departmentRepository.save(new Department("교육부", null, "교육"));
        departmentRepository.save(new Department("총무부", null, "총무"));
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/v1/departments")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.size()", greaterThan(0))
            .body("data.totalCount", greaterThan(0));
    }

    @Test
    @DisplayName("부서 조회 실패 - 권한 없음")
    void getDepartmentsFailUnauthorized() {
        given()
        .when()
            .get("/api/v1/departments")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
