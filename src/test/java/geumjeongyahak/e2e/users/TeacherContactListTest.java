package geumjeongyahak.e2e.users;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

@DisplayName("E2E: 교사 연락망 조회 테스트")
class TeacherContactListTest extends UserBaseTest {

    private static final long TEACHER_CONTACT_SUBJECT_ID = 9190L;

    private String guestAccessToken;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "";
        userTestHelper.createTestUser("teacherContactGuest@test.com", "Teacher Contact Guest", "pw_guest", RoleType.GUEST);
        guestAccessToken = userTestHelper.generateAccessTokenByEmail("teacherContactGuest@test.com");
    }

    @AfterEach
    void cleanUpTeacherContactSubjects() {
        jdbcTemplate.update("DELETE FROM subjects WHERE id = ?", TEACHER_CONTACT_SUBJECT_ID);
    }

    @Test
    @DisplayName("인증 사용자는 현재 활동 중인 교사 연락망을 조회할 수 있다")
    void getTeacherContactList_Success() {
        LocalDate today = LocalDate.now();

        createTeacherContact(
            "contact-current@test.com",
            "Current Teacher",
            "010-1111-2222",
            today.minusDays(1),
            today.plusDays(1)
        );
        createTeacherContact(
            "contact-open-ended@test.com",
            "Open Ended Teacher",
            "010-3333-4444",
            today.minusDays(1),
            null
        );
        createTeacherContact(
            "contact-future@test.com",
            "Future Teacher",
            "010-5555-6666",
            today.plusDays(1),
            today.plusDays(10)
        );
        createTeacherContact(
            "contact-expired@test.com",
            "Expired Teacher",
            "010-7777-8888",
            today.minusDays(10),
            today.minusDays(1)
        );
        User deactivatedTeacher = createTeacherContact(
            "contact-deactivated@test.com",
            "Deactivated Teacher",
            "010-9999-0000",
            today.minusDays(1),
            today.plusDays(1)
        );
        deactivatedTeacher.softDelete();
        userRepository.save(deactivatedTeacher);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/api/v1/teachers/contact-list")
        .then()
            .statusCode(200)
            .body("name", hasItems("Current Teacher", "Open Ended Teacher"))
            .body("name", not(hasItems("Future Teacher", "Expired Teacher")))
            .body("name", not(hasItem("Deactivated Teacher")))
            .body("classroomName", hasItems("벚꽃반"))
            .body("phoneNumber", hasItems("010-1111-2222", "010-3333-4444"));
    }

    @Test
    @DisplayName("교사 연락망은 담당 과목의 분반명을 반환한다")
    void getTeacherContactList_UsesAssignedSubjectClassroom() {
        User teacher = createTeacherContact(
            "contact-assigned-subject@test.com",
            "Assigned Subject Teacher",
            "010-1212-3434",
            LocalDate.now().minusDays(1),
            null
        );
        teacher.setClassroom(null);
        userRepository.save(teacher);
        insertTeacherContactSubject(teacher.getId());

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/api/v1/teachers/contact-list")
        .then()
            .statusCode(200)
            .body("find { it.name == 'Assigned Subject Teacher' }.classroomName", equalTo("장미반"));
    }

    @Test
    @DisplayName("게스트는 교사 연락망을 조회할 수 없다")
    void getTeacherContactList_Forbidden_Guest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
        .when()
            .get("/api/v1/teachers/contact-list")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증 없이 교사 연락망 조회 시 401을 반환한다")
    void getTeacherContactList_Unauthorized() {
        given()
        .when()
            .get("/api/v1/teachers/contact-list")
        .then()
            .statusCode(401);
    }

    private User createTeacherContact(
        String email,
        String name,
        String phoneNumber,
        LocalDate teacherStartAt,
        LocalDate teacherEndAt
    ) {
        User user = userTestHelper.createTestUser(email, name, "pw_" + email, RoleType.VOLUNTEER);
        user.setPhoneNumber(phoneNumber);
        user.setTeacherStartAt(teacherStartAt);
        user.setTeacherEndAt(teacherEndAt);
        user.setClassroom(classroomRepository.findById(1L).orElseThrow());
        userRepository.save(user);
        userTestHelper.setUser(email);
        return user;
    }

    private void insertTeacherContactSubject(Long teacherId) {
        jdbcTemplate.update("""
            MERGE INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            KEY(id)
            VALUES (
                ?, 2, ?, '교사 연락망 배정 과목', DATE '2099-03-02', DATE '2099-06-30', 'MONDAY',
                TIME '19:20:00', TIME '20:00:00', 1, CURRENT_TIMESTAMP, '교사 연락망 테스트', TRUE
            )
            """, TEACHER_CONTACT_SUBJECT_ID, teacherId);
    }
}
