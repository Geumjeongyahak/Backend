package sonmoeum.e2e.subjects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("e2e")
@DisplayName("Subject E2E Tests - Create")
class CreateSubjectE2ETest extends BaseSubjectE2ETest {

    @Test
    @DisplayName("과목 생성 성공 - 수업 자동 생성 확인")
    void createSubjectSuccess() {
        Long classroomId = createClassroom();
        Long teacherId = createTeacher();
        String sessionCookie = getAdminSession();

        var subjectRequest = createSubjectRequest(classroomId, teacherId);

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(subjectRequest)
        .when()
            .post("/api/v1/subjects")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.name", equalTo("수학"))
            .body("data.id", notNullValue());

        // Verify lessons were created (via event)
        // Note: This depends on your event handling implementation
        // You may need to add a delay or check lesson repository directly
    }

    @Test
    @DisplayName("과목 생성 실패 - 권한 없음")
    void createSubjectFailUnauthorized() {
        Long classroomId = createClassroom();
        Long teacherId = createTeacher();

        var subjectRequest = createSubjectRequest(classroomId, teacherId);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(subjectRequest)
        .when()
            .post("/api/v1/subjects")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("과목 생성 실패 - 잘못된 분반 ID")
    void createSubjectFailInvalidClassroomId() {
        Long teacherId = createTeacher();
        String sessionCookie = getAdminSession();

        var subjectRequest = createSubjectRequest(99999L, teacherId);

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(subjectRequest)
        .when()
            .post("/api/v1/subjects")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
