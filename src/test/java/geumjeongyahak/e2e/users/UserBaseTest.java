package geumjeongyahak.e2e.users;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("user")
public abstract class UserBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "volunteer1234";
    public String adminAccessToken;
    public String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/users";
        this.userTestHelper.createTestUser(
            TEST_VOLUNTEER_USERNAME,
            TEST_VOLUNTEER_USERNAME,
            TEST_VOLUNTEER_USERNAME + "@test.com",
            "pw_" + TEST_VOLUNTEER_USERNAME,
            RoleType.VOLUNTEER
        );
        this.adminAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_VOLUNTEER_USERNAME);
    }
}
