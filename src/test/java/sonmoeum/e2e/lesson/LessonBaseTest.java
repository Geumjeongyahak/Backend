package sonmoeum.e2e.lesson;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.e2e.BaseE2ETest;

@Tag("lesson")
public class LessonBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "volunteer1234";
    protected String adminAccessToken;
    protected String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/lessons";
        this.adminAccessToken = userTestHelper.generateAccessToken(TEST_ADMIN_USERNAME);

        this.userTestHelper.createTestUser(TEST_VOLUNTEER_USERNAME, List.of(RoleType.ROLE_VOLUNTEER));
        this.volunteerAccessToken = userTestHelper.generateAccessToken(TEST_VOLUNTEER_USERNAME);
    }
}
