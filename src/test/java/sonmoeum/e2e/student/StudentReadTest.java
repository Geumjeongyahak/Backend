package sonmoeum.e2e.student;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 학생 조회 테스트")
public class StudentReadTest extends StudentBaseTest {

    @Test
    @DisplayName("관리자 권한으로 학생 단건 조회 성공(200 OK)")
    void getStudentById_Success_Admin() {
        Long studentId = 1L; // init_data.sql: TestStudent

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{studentId}", studentId)
            .then()
            .statusCode(200)
            .body("id", equalTo(studentId.intValue()))
            .body("name", equalTo("TestStudent"))
            .body("phoneNumber", equalTo("010-1234-5678"))
            .body("description", equalTo("테스트 학생입니다."))
            .log().all();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 학생 단건 조회 성공(200 OK)")
    void getStudentById_Success_Volunteer() {
        Long studentId = 1L; // init_data.sql: TestStudent

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{studentId}", studentId)
            .then()
            .statusCode(200)
            .body("id", equalTo(studentId.intValue()))
            .body("name", equalTo("TestStudent"))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 학생 조회 실패(404 Not Found)")
    void getStudentById_NotFound() {
        Long nonExistentId = 99999L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{studentId}", nonExistentId)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 학생 단건 조회 실패(401 Unauthorized)")
    void getStudentById_Unauthorized() {
        Long studentId = 1L;

        given()
            .when()
            .get("/{studentId}", studentId)
            .then()
            .statusCode(401)
            .log().all();
    }
}
