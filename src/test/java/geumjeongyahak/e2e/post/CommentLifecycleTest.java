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
import geumjeongyahak.domain.comment.v1.dto.request.CreateCommentRequest;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;

@DisplayName("E2E: Comment 생명주기 테스트")
@ResourceLock("post-e2e-shared-state")
class CommentLifecycleTest extends BasePostTest {

    @Test
    @DisplayName("작성자는 자신의 댓글을 삭제할 수 있고 목록에서 사라진다")
    void deleteComment_asAuthor_removesCommentFromList() {
        String authorToken = createToken("commentAuthorUser1");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken);
        Long commentId = createComment(channelId, postId, authorToken, "삭제 대상 댓글");

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}/comments/{commentId}",
                channelId, postId, commentId)
        .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
        .when()
            .get("/api/v1/channels/{channelId}/posts/{postId}/comments", channelId, postId)
        .then()
            .statusCode(200)
            .body("id", not(hasItems(commentId.intValue())));
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 댓글을 삭제할 수 없다")
    void deleteComment_asOtherUser_returns403() {
        String authorToken = createToken("commentAuthorUser2");
        String otherUserToken = createToken("commentOtherUser2");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken);
        Long commentId = createComment(channelId, postId, authorToken, "권한 확인 댓글");

        given()
            .header(AUTH_HEADER, getAuthHeader(otherUserToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}/comments/{commentId}",
                channelId, postId, commentId)
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("관리자는 다른 사용자의 댓글도 삭제할 수 있다")
    void deleteComment_asAdmin_returns204() {
        String authorToken = createToken("commentAuthorUser3");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken);
        Long commentId = createComment(channelId, postId, authorToken, "관리자 삭제 대상");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}/comments/{commentId}",
                channelId, postId, commentId)
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("대댓글에 다시 답글을 달면 400이 반환된다")
    void createReplyToReply_returns400() {
        String authorToken = createToken("commentAuthorUser4");
        Long channelId = createAllAuthenticatedChannel();
        Long postId = createPost(channelId, authorToken);
        Long parentCommentId = createComment(channelId, postId, authorToken, "부모 댓글");
        Long childCommentId = createReply(channelId, postId, authorToken, parentCommentId, "첫 답글");

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
            .contentType(ContentType.JSON)
            .body(new CreateCommentRequest("답글의 답글", childCommentId))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/comments", channelId, postId)
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"));
    }

    private String createToken(String name) {
        String email = name + "@test.com";
        userTestHelper.createTestUser(email, RoleType.GUEST);
        return userTestHelper.generateAccessTokenByEmail(email);
    }

    private Long createAllAuthenticatedChannel() {
        Channel channel = channelRepository.save(Channel.builder()
            .name("댓글 테스트 게시판")
            .description("Comment lifecycle 테스트 채널")
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

    private Long createPost(Long channelId, String accessToken) {
        Long postId = given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .contentType(ContentType.JSON)
            .body(new CreatePostRequest(
                "댓글 테스트 게시글",
                "<p>댓글 테스트 본문</p>",
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

    private Long createComment(Long channelId, Long postId, String accessToken, String content) {
        Long commentId = given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .contentType(ContentType.JSON)
            .body(new CreateCommentRequest(content, null))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/comments", channelId, postId)
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        testCommentHelper.registerComment(commentId);
        return commentId;
    }

    private Long createReply(
        Long channelId,
        Long postId,
        String accessToken,
        Long parentCommentId,
        String content
    ) {
        Long commentId = given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .contentType(ContentType.JSON)
            .body(new CreateCommentRequest(content, parentCommentId))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/comments", channelId, postId)
        .then()
            .statusCode(201)
            .body("parentCommentId", equalTo(parentCommentId.intValue()))
            .extract()
            .jsonPath()
            .getLong("id");

        testCommentHelper.registerComment(commentId);
        return commentId;
    }
}
