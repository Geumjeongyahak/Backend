package geumjeongyahak.e2e.student;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.student.repository.StudentRepository;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;

@DisplayName("E2E: Student 삭제 테스트")
class StudentDeleteTest extends StudentBaseTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("관리자 권한으로 Student soft delete 성공(204 No Content)")
    void deleteStudent_Success() {
        StudentResponse created = createStudent("Delete Test Student", "010-3333-4444");

        // 삭제 요청
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", created.id())
            .then()
            .statusCode(204)
            .log().all();

        assertThat(studentRepository.findById(created.id()))
            .isPresent()
            .get()
            .satisfies(student -> assertThat(student.isDeleted()).isTrue());

        // 삭제 확인 - 조회 시 404 반환
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{studentId}", created.id())
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("삭제된 Student는 목록에서 제외된다")
    void deleteStudent_ExcludedFromList() {
        StudentResponse created = createStudent("List Hidden Student", "010-3333-5555");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", created.id())
            .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("id", not(hasItem(created.id().intValue())))
            .log().all();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 Student 삭제 실패(403 Forbidden)")
    void deleteStudent_Forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .delete("/{studentId}", 1L)
            .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 Student 삭제 실패(404 Not Found)")
    void deleteStudent_NotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", 99999L)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Student 삭제 실패(401 Unauthorized)")
    void deleteStudent_Unauthorized() {
        given()
            .when()
            .delete("/{studentId}", 1L)
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("같은 Student 두 번 삭제 시 두 번째는 실패(404 Not Found)")
    void deleteStudent_AlreadyDeleted() {
        StudentResponse created = createStudent("Double Delete Student", "010-5555-6666");

        // 첫 번째 삭제
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", created.id())
            .then()
            .statusCode(204);

        // 두 번째 삭제 시도 -> existsById false -> 404
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", created.id())
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("soft delete된 학생과 같은 이름/전화번호로 재등록 성공(201 Created)")
    void createStudent_AfterSoftDeleteWithSameNameAndPhone_Success() {
        String name = "Recreate Student";
        String phoneNumber = "010-7777-9999";
        StudentResponse created = createStudent(name, phoneNumber);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", created.id())
            .then()
            .statusCode(204);

        StudentResponse recreated = createStudent(name, phoneNumber);

        assertThat(recreated.id()).isNotEqualTo(created.id());
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{studentId}", recreated.id())
            .then()
            .statusCode(200)
            .body("name", equalTo(name))
            .body("phoneNumber", equalTo(phoneNumber))
            .log().all();
    }
}
