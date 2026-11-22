package geumjeongyahak.e2e.student;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 학생 분반 소속 해제 테스트")
class StudentClassroomRemoveTest extends StudentBaseTest {

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
    @DisplayName("관리자는 다른 소속을 유지하며 학생의 특정 분반 소속을 해제할 수 있다")
    void removeClassroom_Success_Admin() {
        StudentResponse created = createStudent("분반 해제 학생", "010-1200-0001");
        addClassroom(created.id(), ADDITIONAL_CLASSROOM_ID, adminAccessToken);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{studentId}", created.id())
            .then()
            .statusCode(200)
            .body("classrooms", hasSize(1))
            .body("classrooms[0].id", equalTo(ADDITIONAL_CLASSROOM_ID.intValue()));
    }

    @Test
    @DisplayName("해제한 분반 소속을 다시 추가하면 기존 관계가 활성화된다")
    void removeClassroom_ThenAdd_RestoresRelationship() {
        StudentResponse created = createStudent("분반 해제 후 재추가 학생", "010-1200-0002");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(204);

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
    @DisplayName("student:manage 권한으로 학생의 분반 소속을 해제할 수 있다")
    void removeClassroom_Success_StudentManagePermission() {
        StudentResponse created = createStudent("분반 해제 권한 학생", "010-1200-0003");

        given()
            .header(AUTH_HEADER, getAuthHeader(studentManageAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("student:write 권한만으로는 학생의 분반 소속을 해제할 수 없다")
    void removeClassroom_Forbidden_StudentWritePermission() {
        StudentResponse created = createStudent("분반 해제 쓰기 권한 학생", "010-1200-0011");

        given()
            .header(AUTH_HEADER, getAuthHeader(studentWriteAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"));
    }

    @Test
    @DisplayName("일반 선생님은 학생의 분반 소속을 해제할 수 없다")
    void removeClassroom_Forbidden_Volunteer() {
        StudentResponse created = createStudent("분반 해제 권한 없음 학생", "010-1200-0004");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"));
    }

    @Test
    @DisplayName("인증 없이 학생의 분반 소속을 해제할 수 없다")
    void removeClassroom_Unauthorized() {
        StudentResponse created = createStudent("분반 해제 미인증 학생", "010-1200-0005");

        given()
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("존재하지 않는 학생의 분반 소속 해제는 실패한다")
    void removeClassroom_StudentNotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", 99999L, DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-04-001"));
    }

    @Test
    @DisplayName("존재하지 않는 분반의 학생 소속 해제는 실패한다")
    void removeClassroom_ClassroomNotFound() {
        StudentResponse created = createStudent("분반 없음 해제 학생", "010-1200-0006");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), 99999L)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-03-001"));
    }

    @Test
    @DisplayName("소속되지 않은 분반의 해제는 실패한다")
    void removeClassroom_RelationshipNotFound() {
        StudentResponse created = createStudent("분반 관계 없음 학생", "010-1200-0007");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), ADDITIONAL_CLASSROOM_ID)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-04-002"));
    }

    @Test
    @DisplayName("이미 해제된 분반 소속을 다시 해제하면 실패한다")
    void removeClassroom_AlreadyRemoved() {
        StudentResponse created = createStudent("분반 중복 해제 학생", "010-1200-0008");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-04-002"));
    }

    @Test
    @DisplayName("삭제된 학생의 분반 소속 해제는 실패한다")
    void removeClassroom_DeletedStudent_NotFound() {
        StudentResponse created = createStudent("삭제된 분반 해제 학생", "010-1200-0009");
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}", created.id())
            .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), DEFAULT_CLASSROOM_ID)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-04-001"));
    }

    @Test
    @DisplayName("삭제된 분반의 학생 소속 해제는 실패한다")
    void removeClassroom_DeletedClassroom_NotFound() {
        Classroom deletedClassroom = Classroom.builder()
            .name("삭제된 분반 소속 해제 테스트")
            .type(ClassroomType.WEEKEND)
            .description("삭제된 분반 검증용")
            .build();
        deletedClassroom.setDeleted(true);
        deletedClassroomId = classroomRepository.saveAndFlush(deletedClassroom).getId();
        StudentResponse created = createStudent("삭제 분반 해제 대상 학생", "010-1200-0010");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{studentId}/classrooms/{classroomId}", created.id(), deletedClassroomId)
            .then()
            .statusCode(404)
            .body("code", equalTo("RES-03-001"));
    }

    private void addClassroom(Long studentId, Long classroomId, String accessToken) {
        given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .when()
            .put("/{studentId}/classrooms/{classroomId}", studentId, classroomId)
            .then()
            .statusCode(200);
    }
}
