package geumjeongyahak.e2e.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.RestAssured;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("file")
public abstract class BaseFileTest extends BaseE2ETest {

    protected static final String TEST_FILE_USER = "fileUser1234@test.com";

    protected String userAccessToken;

    @Autowired
    protected FileRepository fileRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/files";

        userTestHelper.createTestUser(TEST_FILE_USER, RoleType.VOLUNTEER);
        userAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_FILE_USER);
    }

    @AfterEach
    @Override
    public void tearDown() {
        fileRepository.deleteAll();
        super.tearDown();
    }
}
