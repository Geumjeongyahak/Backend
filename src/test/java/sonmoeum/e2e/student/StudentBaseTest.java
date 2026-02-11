package sonmoeum.e2e.student;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.student.v1.dto.request.CreateStudentRequest;
import sonmoeum.domain.student.v1.dto.response.StudentResponse;
import sonmoeum.e2e.BaseE2ETest;

@Tag("student")
public abstract class StudentBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "volunteer1234";
    protected String adminAccessToken;
    protected String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/students";
        this.adminAccessToken = userTestHelper.generateAccessToken(TEST_ADMIN_USERNAME);

        this.userTestHelper.createTestUser(TEST_VOLUNTEER_USERNAME, List.of(RoleType.ROLE_VOLUNTEER));
        this.volunteerAccessToken = userTestHelper.generateAccessToken(TEST_VOLUNTEER_USERNAME);
    }

    protected StudentResponse createStudent(String name, String phoneNumber) {
        CreateStudentRequest createReq = new CreateStudentRequest(
            name,
            phoneNumber,
            "E2E seed"
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
