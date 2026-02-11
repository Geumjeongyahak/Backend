package sonmoeum.e2e.department;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.e2e.BaseE2ETest;
import sonmoeum.e2e.util.TestDepartmentHelper;

import java.util.List;

@Tag("department")
public abstract class DepartmentBaseTest extends BaseE2ETest {
    public static final String TEST_VOLUNTEER_USERNAME = "volunteer1234";
    public String adminAccessToken;
    public String volunteerAccessToken;

    @Autowired
    protected TestDepartmentHelper departmentTestHelper;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/departments";

        // volunteer 사용자 생성
        this.userTestHelper.createTestUser(TEST_VOLUNTEER_USERNAME, List.of(RoleType.ROLE_VOLUNTEER));

        // 토큰 생성
        this.adminAccessToken = userTestHelper.generateAccessToken(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessToken(TEST_VOLUNTEER_USERNAME);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        super.tearDown();
        this.departmentTestHelper.clearAll();
    }
}
