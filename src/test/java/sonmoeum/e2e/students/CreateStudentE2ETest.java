package sonmoeum.e2e.students;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("e2e")
@DisplayName("Student E2E Tests - Create")
class CreateStudentE2ETest extends BaseStudentE2ETest {

    @Test
    @DisplayName("학생 생성 성공")
    void createStudentSuccess() {
        String sessionCookie = getAdminSession();
        var studentRequest = createStudentRequest("홍길동", "초등 3학년", "금정초등학교");

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(studentRequest)
        .when()
            .post("/api/v1/students")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.name", equalTo("홍길동"))
            .body("data.grade", equalTo("초등 3학년"))
            .body("data.id", notNullValue());
    }

    @Test
    @DisplayName("학생 생성 실패 - 권한 없음")
    void createStudentFailUnauthorized() {
        var studentRequest = createStudentRequest("홍길동", "초등 3학년", "금정초등학교");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(studentRequest)
        .when()
            .post("/api/v1/students")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
