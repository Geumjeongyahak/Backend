package geumjeongyahak.e2e.users;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

@DisplayName("E2E: 교사 연락망 조회 테스트")
class TeacherContactListTest extends UserBaseTest {

    private String guestAccessToken;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "";
        userTestHelper.createTestUser("teacherContactGuest@test.com", "Teacher Contact Guest", "pw_guest", RoleType.GUEST);
        guestAccessToken = userTestHelper.generateAccessTokenByEmail("teacherContactGuest@test.com");
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
}
