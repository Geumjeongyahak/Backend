package sonmoeum.e2e.student;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.student.v1.dto.request.CreateStudentRequest;

@DisplayName("E2E: Student 생성 테스트")
public class StudentCreateTest extends StudentBaseTest {

    @Test
    @DisplayName("관리자 권한으로 학생 생성 성공(201 Created)")
    void createStudent_Success_Admin() {
        String uniqueName = "학생" + System.currentTimeMillis();
        CreateStudentRequest req = new CreateStudentRequest(
            uniqueName,
            "010-1234-5678",
            "E2E 학생 등록 테스트"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo(uniqueName))
            .body("phoneNumber", equalTo("010-1234-5678"))
            .body("description", equalTo("E2E 학생 등록 테스트"))
            .body("status", equalTo("ENROLLED"))
            .log().all();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 학생 생성 실패(403 Forbidden)")
    void createStudent_Forbidden_Volunteer() {
        CreateStudentRequest req = new CreateStudentRequest(
            "권한없는생성",
            "010-1234-5678",
            "권한 테스트"
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
    @DisplayName("인증 없이 학생 생성 실패(401 Unauthorized)")
    void createStudent_Unauthorized() {
        CreateStudentRequest req = new CreateStudentRequest(
            "인증없음",
            "010-1234-5678",
            "인증 테스트"
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
    @DisplayName("중복된 (이름, 전화번호)로 학생 생성 실패(409 Conflict)")
    void createStudent_DuplicateStudent() {
        String name = "중복학생" + System.currentTimeMillis();
        String phone = "010-9999-8888";

        CreateStudentRequest req = new CreateStudentRequest(name, phone, "중복 1회차");

        // 1회차 생성 성공
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
            .when()
            .post()
            .then()
            .statusCode(201);

        // 2회차 생성 -> 409 기대
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new CreateStudentRequest(name, phone, "중복 2회차"))
            .when()
            .post()
            .then()
            .statusCode(409)
            .body("code", startsWith("BIZ-04-001"))
            .log().all();
    }

    @Test
    @DisplayName("필수 필드 누락 시 학생 생성 실패(400 Bad Request)")
    void createStudent_MissingRequiredFields() {
        String invalidReq = """
            {
                "phoneNumber": "010-1234-5678",
                "description": "name 누락"
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
    @DisplayName("잘못된 전화번호 형식으로 학생 생성 실패(400 Bad Request)")
    void createStudent_InvalidPhoneNumber() {
        CreateStudentRequest req = new CreateStudentRequest(
            "전화번호오류",
            "invalid-phone",
            "전화번호 검증 실패"
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
