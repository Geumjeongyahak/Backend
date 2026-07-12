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
    @DisplayName("교원 역할과 대표 분반이 있는 사용자는 활동 기간과 관계없이 연락망에 포함된다")
    void getTeacherContactList_UsesRoleAndClassroom() {
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

        createTeacherContact(
            "contact-no-period@test.com",
            "No Period Teacher",
            "010-1010-2020",
            null,
            null
        );

        User noClassroomTeacher = createTeacherContact(
            "contact-no-classroom@test.com",
            "No Classroom Teacher",
            "010-3030-4040",
            null,
            null
        );
        noClassroomTeacher.setClassroom(null);
        userRepository.save(noClassroomTeacher);

        User guestWithClassroom = userTestHelper.createTestUser(
            "contact-guest-with-classroom@test.com",
            "Guest With Classroom",
            "pw_guest_classroom",
            RoleType.GUEST
        );
        guestWithClassroom.setPhoneNumber("010-5050-6060");
        guestWithClassroom.setClassroom(classroomRepository.findById(1L).orElseThrow());
        userRepository.save(guestWithClassroom);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/api/v1/teachers/contact-list")
        .then()
            .statusCode(200)
            .body("name", hasItems(
                "Current Teacher",
                "Open Ended Teacher",
                "Future Teacher",
                "Expired Teacher",
                "No Period Teacher"
            ))
            .body("name", not(hasItem("Deactivated Teacher")))
            .body("name", not(hasItem("No Classroom Teacher")))
            .body("name", not(hasItem("Guest With Classroom")))
            .body("classroomName", hasItems("벚꽃반"))
            .body("phoneNumber", hasItems("010-1111-2222", "010-3333-4444", "010-1010-2020"));
    }

    @Test
    @DisplayName("대표 분반이 없으면 활성 과목이 있어도 연락망에서 제외된다")
    void getTeacherContactList_ExcludesTeacherWithoutRepresentativeClassroom() {
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
            .body("name", not(hasItem("Assigned Subject Teacher")));
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
