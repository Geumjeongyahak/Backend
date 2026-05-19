package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;
import geumjeongyahak.domain.post.v1.dto.request.UpdatePostRequest;

@DisplayName("E2E: Post 생명주기 테스트")
@ResourceLock("post-e2e-shared-state")
class PostLifecycleTest extends BasePostTest {

    @Test
    @DisplayName("작성자는 게시글을 수정할 수 있다")
    void updatePost_asAuthor_returnsUpdatedPost() {
        String authorToken = createToken("postAuthorUser1");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken, "수정 전 제목");

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
            .contentType(ContentType.JSON)
            .body(new UpdatePostRequest(
                "수정 후 제목",
                "<p>수정된 본문</p>",
                "PUBLISHED",
                false,
                null
            ))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}", channelId, postId)
        .then()
            .statusCode(200)
            .body("id", equalTo(postId.intValue()))
            .body("title", equalTo("수정 후 제목"))
            .body("contentHtml", equalTo("<p>수정된 본문</p>"))
            .body("allowComment", equalTo(false));
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 게시글을 수정할 수 없다")
    void updatePost_asOtherUser_returns403() {
        String authorToken = createToken("postAuthorUser2");
        String otherUserToken = createToken("postOtherUser2");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken, "권한 확인 게시글");

        given()
            .header(AUTH_HEADER, getAuthHeader(otherUserToken))
            .contentType(ContentType.JSON)
            .body(new UpdatePostRequest("수정 시도", null, null, null, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}", channelId, postId)
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("작성자는 게시글을 삭제하면 목록과 상세에서 제외된다")
    void deletePost_asAuthor_removesPostFromReadApis() {
        String authorToken = createToken("postAuthorUser3");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken, "삭제 대상 게시글");

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", channelId, postId)
        .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
        .when()
            .get("/api/v1/channels/{channelId}/posts/{postId}", channelId, postId)
        .then()
            .statusCode(404);

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
        .when()
            .get("/api/v1/channels/{channelId}/posts", channelId)
        .then()
            .statusCode(200)
            .body("content.id", not(hasItems(postId.intValue())));
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 게시글을 삭제할 수 없다")
    void deletePost_asOtherUser_returns403() {
        String authorToken = createToken("postAuthorUser4");
        String otherUserToken = createToken("postOtherUser4");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken, "삭제 권한 테스트");

        given()
            .header(AUTH_HEADER, getAuthHeader(otherUserToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", channelId, postId)
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("관리자는 다른 사용자의 게시글도 삭제할 수 있다")
    void deletePost_asAdmin_returns204() {
        String authorToken = createToken("postAuthorUser5");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken, "관리자 삭제 대상");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", channelId, postId)
        .then()
            .statusCode(204);
    }

    private String createToken(String username) {
        userTestHelper.createTestUser(username, RoleType.GUEST);
        return userTestHelper.generateAccessTokenByUserKey(username);
    }

    private Long createAllAuthenticatedChannel() {
        Channel channel = channelRepository.save(Channel.builder()
            .name("전체 사용자 게시판")
            .description("Post lifecycle 테스트 채널")
            .channelType(ChannelType.CUSTOM)
            .bindingType(ChannelBindingType.STANDALONE)
            .accessLevel(ChannelAccessLevel.READ_WRITE)
            .refId(null)
            .isDefault(false)
            .isActive(true)
            .build());
        testChannelHelper.registerChannel(channel.getId());
        return channel.getId();
    }

    private Long createPost(Long channelId, String accessToken, String title) {
        Long postId = given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .contentType(ContentType.JSON)
            .body(new CreatePostRequest(
                title,
                "<p>" + title + " 본문</p>",
                "PUBLISHED",
                false,
                true,
                null
            ))
        .when()
            .post("/api/v1/channels/{channelId}/posts", channelId)
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        testPostHelper.registerPost(postId);
        return postId;
    }
}
