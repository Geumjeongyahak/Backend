package geumjeongyahak.e2e.student;

import static geumjeongyahak.domain.student.enums.StudentStatus.ON_LEAVE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.student.repository.StudentRepository;
import geumjeongyahak.domain.student.v1.dto.request.CreateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.request.UpdateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;

@DisplayName("E2E: 학생 목록 조회 테스트")
class StudentListReadTest extends StudentBaseTest {

    @Autowired
    private DailyStudentAttendanceRepository dailyStudentAttendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        dailyStudentAttendanceRepository.deleteAll();
        studentRepository.deleteAll();
        createSeedStudents();
    }

    private void createSeedStudents() {
        createTestStudent("Charlie Student", "010-1000-0003", DEFAULT_CLASSROOM_ID);
        createTestStudent("Alpha Student", "010-1000-0001", DEFAULT_CLASSROOM_ID);
        createTestStudent("Bravo Student", "010-1000-0002", DEFAULT_CLASSROOM_ID);
        StudentResponse daisy = createTestStudent("Daisy Student", "010-1000-0004", DEFAULT_CLASSROOM_ID);
        createTestStudent("Rose Student", "010-1000-0005", 2L);

        UpdateStudentRequest updateReq = new UpdateStudentRequest(null, null, null, ON_LEAVE, null);
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
            .when()
            .patch("/{studentId}", daisy.id())
            .then()
            .statusCode(200);
    }

    private StudentResponse createTestStudent(String name, String phoneNumber, Long classroomId) {
        CreateStudentRequest req = new CreateStudentRequest(
            name,
            phoneNumber,
            "학생 목록 조회 테스트",
            classroomId
        );

        return given()
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

    @Test
    @DisplayName("학생 목록을 배열로 이름순 조회 성공(200 OK) - 관리자")
    void getAllStudents_AsArraySortedByName_Admin() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .body("size()", equalTo(5))
            .body("name", contains(
                "Alpha Student",
                "Bravo Student",
                "Charlie Student",
                "Daisy Student",
                "Rose Student"
            ))
            .log().all();
    }

    @Test
    @DisplayName("학생 목록을 배열로 이름순 조회 성공(200 OK) - 일반 선생님")
    void getAllStudents_AsArraySortedByName_Volunteer() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("size()", equalTo(5))
            .body("name", contains(
                "Alpha Student",
                "Bravo Student",
                "Charlie Student",
                "Daisy Student",
                "Rose Student"
            ))
            .log().all();
    }

    @Test
    @DisplayName("이름 필터 조회 성공(200 OK)")
    void getAllStudents_FilterByName() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("name", "Student")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("size()", equalTo(5))
            .body("name", everyItem(containsString("Student")))
            .log().all();
    }

    @Test
    @DisplayName("상태 필터 조회 성공(200 OK)")
    void getAllStudents_FilterByStatus() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("status", "ON_LEAVE")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("status", everyItem(is("ON_LEAVE")))
            .body("[0].name", equalTo("Daisy Student"))
            .log().all();
    }

    @Test
    @DisplayName("분반 필터 조회 성공(200 OK)")
    void getAllStudents_FilterByClassroomId() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("classroomId", 2L)
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("classroomId", everyItem(is(2)))
            .body("[0].classroomName", equalTo("장미반"))
            .log().all();
    }

    @Test
    @DisplayName("이름+상태+분반 조합 필터 조회 성공(200 OK)")
    void getAllStudents_FilterByNameStatusAndClassroomId() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("name", "Daisy")
            .queryParam("status", "ON_LEAVE")
            .queryParam("classroomId", DEFAULT_CLASSROOM_ID)
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].name", equalTo("Daisy Student"))
            .body("[0].status", equalTo("ON_LEAVE"))
            .body("[0].classroomId", equalTo(DEFAULT_CLASSROOM_ID.intValue()))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 분반 필터 조회 실패(404 Not Found)")
    void getAllStudents_ClassroomNotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("classroomId", 99999L)
            .when()
            .get()
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-03-001"))
            .log().all();
    }

    @Test
    @DisplayName("게스트 권한으로 목록 조회 실패(403 Forbidden)")
    void getAllStudents_Forbidden_Guest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
            .when()
            .get()
            .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 목록 조회 실패(401 Unauthorized)")
    void getAllStudents_Unauthorized() {
        given()
            .when()
            .get()
            .then()
            .statusCode(401)
            .log().all();
    }
}
