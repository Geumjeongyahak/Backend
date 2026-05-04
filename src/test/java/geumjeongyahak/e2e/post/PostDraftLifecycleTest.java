package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import geumjeongyahak.domain.post.v1.dto.request.SaveDraftRequest;
import geumjeongyahak.domain.post.v1.dto.request.PublishPostRequest;

@DisplayName("E2E: Post 초안 라이프사이클 테스트")
@ResourceLock("post-e2e-shared-state")
class PostDraftLifecycleTest extends BasePostTest {

    @Test
    @DisplayName("초안 생성 후 임시 저장하고 파일을 연동하여 발행할 수 있다")
    void postDraftLifecycle_Success() {
        // 1. 초안 생성
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("status", equalTo("DRAFT"))
            .body("title", equalTo(""))
            .body("contentHtml", equalTo(""))
            .body("expiresAt", notNullValue());

        // 2. 임시 저장
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new SaveDraftRequest("초안 제목", "<p>초안 본문</p>", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/draft", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("title", equalTo("초안 제목"))
            .body("contentHtml", equalTo("<p>초안 본문</p>"));

        // 3. 첨부파일 연동
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartAttachmentRequest("file", "test.pdf"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/draft/attachments", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("originalName", equalTo("test.pdf"));

        // 4. 발행
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PublishPostRequest("최종 제목", "<p>최종 본문</p>", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/publish", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("status", equalTo("PUBLISHED"))
            .body("title", equalTo("최종 제목"))
            .body("attachments.size()", equalTo(1))
            .body("attachments[0].originalName", equalTo("test.pdf"));
    }

    @Test
    @DisplayName("초안 생성 권한 - GUEST 사용자는 READ_ONLY 채널에 초안을 생성할 수 없다")
    void createDraft_GuestOnReadOnly_Forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
            .contentType(ContentType.JSON)
        .when()
            .post("/api/v1/channels/{channelId}/posts/drafts", noticeChannelId)
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("초안 저장 권한 - 타인의 초안을 저장하거나 상태가 DRAFT가 아니면 실패한다")
    void saveDraft_Constraints() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        // 1. 타인이 저장 시도 -> 403
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
            .contentType(ContentType.JSON)
            .body(new SaveDraftRequest("제목", "내용", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/draft", noticeChannelId, postId)
        .then()
            .statusCode(403);

        // 2. 이미 발행된 게시글을 초안 저장 API로 접근 -> 400
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PublishPostRequest("발행", "내용", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/publish", noticeChannelId, postId)
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new SaveDraftRequest("이미 발행됨", "내용", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/draft", noticeChannelId, postId)
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("발행 시 필수 필드 검증 - 제목이나 본문이 없으면 발행할 수 없다")
    void publish_Validation() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        // 제목 누락
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PublishPostRequest("", "<p>본문</p>", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/publish", noticeChannelId, postId)
        .then()
            .statusCode(400);

        // 본문 누락
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PublishPostRequest("제목", "", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/publish", noticeChannelId, postId)
        .then()
            .statusCode(400);
    }
}
