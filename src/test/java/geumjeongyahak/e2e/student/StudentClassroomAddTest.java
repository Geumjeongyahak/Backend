package geumjeongyahak.e2e.student;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.student.enums.StudentStatus;
import geumjeongyahak.domain.student.v1.dto.request.UpdateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 학생 분반 소속 추가 테스트")
class StudentClassroomAddTest extends StudentBaseTest {

    private static final Long ADDITIONAL_CLASSROOM_ID = 2L;

    @Autowired
    private ClassroomRepository classroomRepository;

    private Long deletedClassroomId;

    @AfterEach
    void cleanupClassroomFixture() {
        if (deletedClassroomId != null) {
            classroomRepository.deleteById(deletedClassroomId);
        }
    }

    @Test
    @DisplayName("관리자는 기존 소속을 유지하며 학생을 분반에 추가할 수 있다")
    void addClassroom_Success_Admin() {
        StudentResponse created = createStudent("분반 추가 학생", "010-1100-0001");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(200)
            .body("id", equalTo(created.id().intValue()))
            .body("classrooms", hasSize(2))
            .body("classrooms.id", containsInAnyOrder(
                DEFAULT_CLASSROOM_ID.intValue(),
                ADDITIONAL_CLASSROOM_ID.intValue()
            ));
    }

    @Test
    @DisplayName("이미 소속된 분반을 다시 추가해도 관계가 중복되지 않는다")
    void addClassroom_AlreadyAssigned_Idempotent() {
        StudentResponse created = createStudent("중복 분반 추가 학생", "010-1100-0002");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(200)
            .body("classrooms", hasSize(1))
            .body("classrooms[0].id", equalTo(DEFAULT_CLASSROOM_ID.intValue()));
    }

    @Test
    @DisplayName("해제된 학생-분반 관계를 다시 추가하면 관계가 활성화된다")
    void addClassroom_SoftDeletedRelationship_Restored() {
        StudentResponse created = createStudent("분반 재추가 학생", "010-1100-0003");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(200);

        UpdateStudentRequest keepDefaultClassroomOnly = new UpdateStudentRequest(
            null,
            null,
            null,
            null,
            List.of(DEFAULT_CLASSROOM_ID)
        );
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(keepDefaultClassroomOnly)
            .when()
            .patch("/{studentId}", created.id())
            .then()
            .statusCode(200)
            .body("classrooms", hasSize(1));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(200)
            .body("classrooms", hasSize(2))
            .body("classrooms.id", containsInAnyOrder(
                DEFAULT_CLASSROOM_ID.intValue(),
                ADDITIONAL_CLASSROOM_ID.intValue()
            ));
    }

    @Test
    @DisplayName("student:write 권한으로 학생을 분반에 추가할 수 있다")
    void addClassroom_Success_StudentWritePermission() {
        StudentResponse created = createStudent("분반 추가 권한 학생", "010-1100-0004");

        given()
            .header(AUTH_HEADER, getAuthHeader(studentWriteAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(200)
            .body("classrooms.id", containsInAnyOrder(
                DEFAULT_CLASSROOM_ID.intValue(),
                ADDITIONAL_CLASSROOM_ID.intValue()
            ));
    }

    @Test
    @DisplayName("일반 선생님은 학생을 분반에 추가할 수 없다")
    void addClassroom_Forbidden_Volunteer() {
        StudentResponse created = createStudent("분반 추가 권한 없음 학생", "010-1100-0005");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"));
    }

    @Test
    @DisplayName("인증 없이 학생을 분반에 추가할 수 없다")
    void addClassroom_Unauthorized() {
        StudentResponse created = createStudent("분반 추가 미인증 학생", "010-1100-0006");

        given()
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("존재하지 않는 학생의 분반 소속 추가는 실패한다")
    void addClassroom_StudentNotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", 99999L, ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-04-001"));
    }

    @Test
    @DisplayName("존재하지 않는 분반의 학생 소속 추가는 실패한다")
    void addClassroom_ClassroomNotFound() {
        StudentResponse created = createStudent("분반 없음 추가 학생", "010-1100-0007");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), 99999L)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-03-001"));
    }

    @Test
    @DisplayName("삭제된 학생의 분반 소속 추가는 실패한다")
    void addClassroom_DeletedStudent_NotFound() {
        StudentResponse created = createStudent("삭제된 분반 추가 학생", "010-1100-0008");
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", created.id())
            .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-04-001"));
    }

    @Test
    @DisplayName("삭제된 분반의 학생 소속 추가는 실패한다")
    void addClassroom_DeletedClassroom_NotFound() {
        Classroom deletedClassroom = Classroom.builder()
            .name("삭제된 분반 소속 추가 테스트")
            .type(ClassroomType.WEEKEND)
            .description("삭제된 분반 검증용")
            .build();
        deletedClassroom.setDeleted(true);
        deletedClassroomId = classroomRepository.saveAndFlush(deletedClassroom).getId();
        StudentResponse created = createStudent("삭제 분반 대상 학생", "010-1100-0009");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), deletedClassroomId)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-03-001"));
    }

    @ParameterizedTest
    @EnumSource(value = StudentStatus.class, names = {"ON_LEAVE", "COMPLETED"})
    @DisplayName("재학 중이 아닌 학생도 분반에 추가할 수 있다")
    void addClassroom_NonEnrolledStudent_Success(StudentStatus status) {
        String phoneNumber = status == StudentStatus.ON_LEAVE
            ? "010-1100-0010"
            : "010-1100-0011";
        StudentResponse created = createStudent("상태별 분반 추가 학생 " + status, phoneNumber);
        UpdateStudentRequest updateStatus = new UpdateStudentRequest(
            null,
            null,
            null,
            status,
            null
        );
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateStatus)
            .when()
            .patch("/{studentId}", created.id())
            .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(200)
            .body("status", equalTo(status.name()))
            .body("classrooms.id", containsInAnyOrder(
                DEFAULT_CLASSROOM_ID.intValue(),
                ADDITIONAL_CLASSROOM_ID.intValue()
            ));
    }
}
