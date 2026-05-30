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

    protected static final String TEST_FILE_USER = "fileUser1234";
    protected static final String TEST_FILE_GUEST = "fileGuest1234";

    protected String userAccessToken;
    protected String guestAccessToken;

    @Autowired
    protected FileRepository fileRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/files";

        userTestHelper.createTestUser(TEST_FILE_USER, RoleType.VOLUNTEER);
        userTestHelper.createTestUser(TEST_FILE_GUEST, RoleType.GUEST);
        userAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_FILE_USER);
        guestAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_FILE_GUEST);
    }

    @AfterEach
    @Override
    public void tearDown() {
        fileRepository.deleteAll();
        super.tearDown();
    }
}
