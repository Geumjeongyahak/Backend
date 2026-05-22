package geumjeongyahak.e2e.users;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("user")
public abstract class UserBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "volunteer1234@test.com";
    public String adminAccessToken;
    public String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/users";
        this.userTestHelper.createTestUser(
            TEST_VOLUNTEER_USERNAME,
            "Volunteer User",
            "pw_volunteer",
            RoleType.VOLUNTEER
        );
        this.adminAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_ADMIN_EMAIL);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_VOLUNTEER_USERNAME);
    }
}
