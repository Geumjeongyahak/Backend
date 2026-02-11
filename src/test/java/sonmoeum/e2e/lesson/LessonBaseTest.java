package sonmoeum.e2e.lesson;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import sonmoeum.e2e.BaseE2ETest;

@Tag("lesson")
public class LessonBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "teacher01";
    protected String adminAccessToken;
    protected String volunteerAccessToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/lessons";
        this.adminAccessToken = userTestHelper.generateAccessToken(TEST_ADMIN_USERNAME);

        this.volunteerAccessToken = userTestHelper.generateAccessToken(TEST_VOLUNTEER_USERNAME);
    }
}
