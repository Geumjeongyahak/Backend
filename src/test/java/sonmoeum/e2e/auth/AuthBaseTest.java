package sonmoeum.e2e.auth;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import sonmoeum.e2e.BaseE2ETest;

@Tag("auth")
public abstract class AuthBaseTest extends BaseE2ETest {

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/auth";
    }
}
