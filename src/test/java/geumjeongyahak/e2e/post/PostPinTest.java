package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import geumjeongyahak.domain.post.v1.dto.request.PinPostRequest;

@DisplayName("E2E: Post 고정(Pin) 테스트")
@ResourceLock("post-e2e-shared-state")
class PostPinTest extends BasePostTest {

    @Test
    @DisplayName("관리자는 게시글을 고정하거나 해제할 수 있다")
    void pinPost_asAdmin_Success() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        // 발행 먼저 (pin은 published 글에 하는 게 일반적이지만 draft도 가능은 할 수 있음. 정책 확인 필요)
        // 일단 직접 생성 API로 Published 글 생성 (기존 테스트 방식)
        postId = createPublishedPost("고정 테스트용");

        // 1. 고정
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PinPostRequest(true))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/pin", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("isPinned", equalTo(true));

        // 2. 고정 해제
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PinPostRequest(false))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/pin", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("isPinned", equalTo(false));
    }

    @Test
    @DisplayName("일반 사용자는 게시글을 고정할 수 없다")
    void pinPost_asGuest_Forbidden() {
        Long postId = createPublishedPost("고정 권한 테스트");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
            .contentType(ContentType.JSON)
            .body(new PinPostRequest(true))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/pin", noticeChannelId, postId)
        .then()
            .statusCode(403);
    }

    private Long createPublishedPost(String title) {
        Long postId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest(
                title,
                "<p>본문</p>",
                "PUBLISHED",
                false,
                true,
                null
            ))
        .when()
            .post("/api/v1/channels/{channelId}/posts", noticeChannelId)
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        testPostHelper.registerPost(postId);
        return postId;
    }
}
