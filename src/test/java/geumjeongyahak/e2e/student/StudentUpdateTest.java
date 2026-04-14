package geumjeongyahak.e2e.student;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static geumjeongyahak.domain.student.enums.StudentStatus.ON_LEAVE;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.student.v1.dto.request.UpdateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;

@DisplayName("E2E: Student 수정 테스트")
class StudentUpdateTest extends StudentBaseTest {

    @Test
    @DisplayName("관리자 권한으로 학생 정보 수정 성공(200 OK) - 부분 수정(PATCH)")
    void updateStudent_Success_Admin() {
        // 학생 생성(관리자만 생성 가능)
        StudentResponse created = createStudent("Update Test Student", "010-1111-2222");

        // 일부 필드만 수정: name, description
        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            "Updated Student Name",
            null,
            "updated description",
            null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
            .when()
            .patch("/{studentId}", created.id())
            .then()
            .statusCode(200)
            .body("id", equalTo(created.id().intValue()))
            .body("name", equalTo("Updated Student Name"))
            // phoneNumber는 null로 보내서 기존 값 유지
            .body("phoneNumber", equalTo("010-1111-2222"))
            .body("description", equalTo("updated description"))
            .log().all();
    }

    @Test
    @DisplayName("관리자 권한으로 status 수정 성공(200 OK)")
    void updateStudent_Status_Success_Admin() {
        StudentResponse created = createStudent("Status Student", "010-2222-3333");

        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            null,
            null,
            null,
            ON_LEAVE
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
            .when()
            .patch("/{studentId}", created.id())
            .then()
            .statusCode(200)
            .body("id", equalTo(created.id().intValue()))
            .body("status", equalTo("ON_LEAVE"))
            .log().all();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 학생 수정 실패(403 Forbidden)")
    void updateStudent_Forbidden_Volunteer() {
        StudentResponse created = createStudent("Forbidden Student", "010-3333-4444");

        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            "Hacker Name",
            null,
            null,
            null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
            .when()
            .patch("/{studentId}", created.id())
            .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 학생 수정 실패(404 Not Found)")
    void updateStudent_NotFound() {
        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            "New Name",
            null,
            null,
            null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
            .when()
            .patch("/{studentId}", 99999L)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("수정 시 (이름, 전화번호) 조합이 이미 존재하면 실패(409 Conflict)")
    void updateStudent_DuplicateNameAndPhone_Conflict() {
        // A 학생
        StudentResponse a = createStudent("Dup A", "010-4444-5555");
        // B 학생
        StudentResponse b = createStudent("Dup B", "010-6666-7777");

        // B를 A의 (이름,전화번호) 조합으로 변경 시도
        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            a.name(),
            a.phoneNumber(),
            null,
            null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
            .when()
            .patch("/{studentId}", b.id())
            .then()
            .statusCode(409)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 학생 수정 실패(401 Unauthorized)")
    void updateStudent_Unauthorized() {
        StudentResponse created = createStudent("Unauth Student", "010-7777-8888");

        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            "New Name",
            null,
            null,
            null
        );

        given()
            .contentType(ContentType.JSON)
            .body(updateReq)
            .when()
            .patch("/{studentId}", created.id())
            .then()
            .statusCode(401)
            .log().all();
    }
}
