package geumjeongyahak.e2e.student;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.student.v1.dto.request.CreateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("student")
public abstract class StudentBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "volunteer1234@test.com";
    public static final String TEST_GUEST_USERNAME = "studentGuest1234@test.com";
    public static final String TEST_STUDENT_WRITER_USERNAME = "studentWriter1234@test.com";
    protected static final Long DEFAULT_CLASSROOM_ID = 1L;
    protected static final String DEFAULT_CLASSROOM_NAME = "벚꽃반";
    private static final String STUDENT_WRITE_PERMISSION = "student:write:*";

    protected String adminAccessToken;
    protected String volunteerAccessToken;
    protected String guestAccessToken;
    protected String studentWriteAccessToken;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/students";
        this.adminAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_ADMIN_USERNAME);

        this.userTestHelper.createTestUser(TEST_VOLUNTEER_USERNAME, RoleType.VOLUNTEER);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_VOLUNTEER_USERNAME);

        this.userTestHelper.createTestUser(TEST_GUEST_USERNAME, RoleType.GUEST);
        this.guestAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_GUEST_USERNAME);

        User studentWriter = this.userTestHelper.createTestUser(TEST_STUDENT_WRITER_USERNAME, RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(studentWriter.getId(), STUDENT_WRITE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(studentWriter, STUDENT_WRITE_PERMISSION)));
        this.studentWriteAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_STUDENT_WRITER_USERNAME);
    }

    protected StudentResponse createStudent(String name, String phoneNumber) {
        CreateStudentRequest createReq = new CreateStudentRequest(
            name,
            phoneNumber,
            "E2E seed",
            DEFAULT_CLASSROOM_ID
        );

        return given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
            .when()
            .post()
            .then()
            .statusCode(201)
            .extract()
            .as(StudentResponse.class);
    }
}
