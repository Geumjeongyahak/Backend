package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.restassured.response.Response;
import geumjeongyahak.e2e.util.AdminSessionHelper;
import geumjeongyahak.e2e.util.AdminSessionHelper.AdminSession;

@DisplayName("E2E: Post 관리자 파일 화면 테스트")
@ResourceLock("post-e2e-shared-state")
class PostAdminViewFileTest extends BasePostTest {

    @Test
    @DisplayName("관리자 화면에서 첨부파일을 추가하고 게시글 상세에서 확인할 수 있다")
    void adminAttachAttachment_Success() {
        AdminSession session = loginAdminSession();
        Long postId = createAdminDraft(session);

        given()
            .cookie("JSESSIONID", session.sessionId())
            .formParam("_csrf", session.csrfToken())
            .multiPart(testFileHelper.multipartAttachmentRequest("file", "admin-attachment.pdf"))
        .when()
            .post("/admin/channel/{channelId}/posts/{postId}/attachments", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("originalName", equalTo("admin-attachment.pdf"));

        given()
            .cookie("JSESSIONID", session.sessionId())
        .when()
            .get("/admin/channel/{channelId}/posts/{postId}", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("admin-attachment.pdf"));
    }

    @Test
    @DisplayName("관리자 화면에서 첨부파일을 삭제하면 게시글 연결도 함께 해제된다")
    void adminDetachAttachment_RemovesPostMapping() {
        AdminSession session = loginAdminSession();
        Long postId = createAdminDraft(session);

        String fileId = given()
            .cookie("JSESSIONID", session.sessionId())
            .formParam("_csrf", session.csrfToken())
            .multiPart(testFileHelper.multipartAttachmentRequest("file", "delete-me.pdf"))
        .when()
            .post("/admin/channel/{channelId}/posts/{postId}/attachments", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("fileId");

        given()
            .cookie("JSESSIONID", session.sessionId())
            .header("X-CSRF-TOKEN", session.csrfToken())
        .when()
            .delete("/admin/channel/{channelId}/posts/{postId}/attachments/{fileId}", noticeChannelId, postId, fileId)
        .then()
            .statusCode(204);

        given()
            .cookie("JSESSIONID", session.sessionId())
        .when()
            .get("/admin/channel/{channelId}/posts/{postId}", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body(not(containsString("delete-me.pdf")));
    }

    @Test
    @DisplayName("파일이 먼저 삭제된 첨부파일도 관리자 화면에서 연결을 해제할 수 있다")
    void adminDetachAttachment_WhenFileAlreadyDeleted_RemovesPostMapping() {
        AdminSession session = loginAdminSession();
        Long postId = createAdminDraft(session);

        String fileId = given()
            .cookie("JSESSIONID", session.sessionId())
            .formParam("_csrf", session.csrfToken())
            .multiPart(testFileHelper.multipartAttachmentRequest("file", "stale-file.pdf"))
        .when()
            .post("/admin/channel/{channelId}/posts/{postId}/attachments", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("fileId");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/files/attachments/{fileId}", fileId)
        .then()
            .statusCode(204);

        given()
            .cookie("JSESSIONID", session.sessionId())
            .header("X-CSRF-TOKEN", session.csrfToken())
        .when()
            .delete("/admin/channel/{channelId}/posts/{postId}/attachments/{fileId}", noticeChannelId, postId, fileId)
        .then()
            .statusCode(204);
    }

    private AdminSession loginAdminSession() {
        return AdminSessionHelper.login(
            TEST_POST_ADMIN_USERNAME + "@test.com",
            userTestHelper.getDefaultPassword(TEST_POST_ADMIN_USERNAME)
        );
    }

    private Long createAdminDraft(AdminSession session) {
        Response response = given()
            .cookie("JSESSIONID", session.sessionId())
            .formParam("_csrf", session.csrfToken())
            .redirects()
            .follow(false)
        .when()
            .post("/admin/channel/{channelId}/posts/drafts", noticeChannelId)
        .then()
            .statusCode(302)
            .extract()
            .response();

        String location = response.header("Location");
        String prefix = "/admin/channel/" + noticeChannelId + "/posts/";
        assertThat(location).contains(prefix).endsWith("/edit");
        Long postId = Long.valueOf(location.substring(location.indexOf(prefix) + prefix.length(), location.lastIndexOf("/edit")));
        testPostHelper.registerPost(postId);
        return postId;
    }
}
