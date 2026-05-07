package geumjeongyahak.e2e.student;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.student.v1.dto.request.CreateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("student")
public abstract class StudentBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "volunteer1234";
    protected static final Long DEFAULT_CLASSROOM_ID = 1L;
    protected static final String DEFAULT_CLASSROOM_NAME = "벚꽃반";
    protected String adminAccessToken;
    protected String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/students";
        this.adminAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_ADMIN_USERNAME);

        this.userTestHelper.createTestUser(TEST_VOLUNTEER_USERNAME, RoleType.VOLUNTEER);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_VOLUNTEER_USERNAME);
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
