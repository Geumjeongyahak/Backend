package sonmoeum.e2e.student;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import sonmoeum.domain.student.repository.StudentRepository;
import sonmoeum.domain.student.v1.dto.request.CreateStudentRequest;
import sonmoeum.domain.student.v1.dto.response.StudentResponse;

@DisplayName("E2E: 학생 페이지네이션 테스트")
class StudentPaginationTest extends StudentBaseTest {

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        studentRepository.deleteAll();
        createTestStudents(15);
    }

    // 테스트 학생 생성 메서드
    private void createTestStudents(int count) {
        for (int i = 1; i <= count; i++) {
            CreateStudentRequest req = new CreateStudentRequest(
                "Page Test Student " + i,
                "010-" + String.format("%04d", i) + "-5678",
                "학생 페이지네이션 테스트 " + i
            );

            given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .as(StudentResponse.class);
        }
    }

    @Test
    @DisplayName("기본 페이지네이션 조회 성공(200 OK) - 관리자")
    void getAllStudents_DefaultPagination_Admin() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(0))
            .body("size", greaterThan(0))
            .body("totalElements", greaterThan(0))
            .body("totalPages", greaterThan(0))
            .log().all();
    }

    @Test
    @DisplayName("기본 페이지네이션 조회 성공(200 OK) - 일반 선생님")
    void getAllStudents_DefaultPagination_Volunteer() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(0))
            .body("size", greaterThan(0))
            .body("totalElements", greaterThan(0))
            .log().all();
    }

    @Test
    @DisplayName("페이지 크기 지정 조회 성공(200 OK)")
    void getAllStudents_WithPageSize() {
        int pageSize = 5;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("size", pageSize)
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", lessThanOrEqualTo(pageSize))
            .body("size", equalTo(pageSize))
            .body("page", equalTo(0))
            .log().all();
    }

    @Test
    @DisplayName("두 번째 페이지 조회 성공(200 OK)")
    void getAllStudents_SecondPage() {
        int pageSize = 5;
        int pageNumber = 1;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("page", pageNumber)
            .queryParam("size", pageSize)
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(pageNumber))
            .body("size", equalTo(pageSize))
            .log().all();
    }

    @Test
    @DisplayName("범위를 벗어난 페이지 조회 시 빈 결과 반환(200 OK)")
    void getAllStudents_OutOfRangePage() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("page", 9999)
            .queryParam("size", 10)
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", equalTo(0))
            .body("page", equalTo(9999))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 페이지네이션 조회 실패(401 Unauthorized)")
    void getAllStudents_Unauthorized() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get()
            .then()
            .statusCode(401)
            .log().all();
    }
}
