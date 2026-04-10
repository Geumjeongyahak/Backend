package sonmoeum.e2e.post;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.post.v1.dto.request.CreatePostRequest;
import sonmoeum.domain.comment.v1.dto.request.CreateCommentRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("E2E: Post 하위 Comment 리소스 테스트")
public class CommentNestedResourceTest extends BasePostTest {

    @Test
    @DisplayName("관리자는 게시글에 댓글과 답글을 작성하고 조회할 수 있다")
    void createAndReadComments_Success() {
        Long postId = createPost(true);

        Long parentCommentId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(new CreateCommentRequest("첫 댓글입니다.", null))
                .when()
                .post("/api/v1/channels/{channelId}/posts/{postId}/comments", noticeChannelId, postId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("postId", equalTo(postId.intValue()))
                .body("parentCommentId", nullValue())
                .body("content", equalTo("첫 댓글입니다."))
                .extract()
                .jsonPath()
                .getLong("id");

        testCommentHelper.registerComment(parentCommentId);

        Long childCommentId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(new CreateCommentRequest("답글입니다.", parentCommentId))
                .when()
                .post("/api/v1/channels/{channelId}/posts/{postId}/comments", noticeChannelId, postId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("parentCommentId", equalTo(parentCommentId.intValue()))
                .body("content", equalTo("답글입니다."))
                .extract()
                .jsonPath()
                .getLong("id");

        testCommentHelper.registerComment(childCommentId);

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}/comments", noticeChannelId, postId)
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].content", equalTo("첫 댓글입니다."))
                .body("[1].parentCommentId", equalTo(parentCommentId.intValue()));
    }

    @Test
    @DisplayName("댓글이 비활성화된 게시글에는 댓글을 작성할 수 없다")
    void createComment_WhenCommentDisabled_BadRequest() {
        Long postId = createPost(false);

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(new CreateCommentRequest("댓글 시도", null))
                .when()
                .post("/api/v1/channels/{channelId}/posts/{postId}/comments", noticeChannelId, postId)
                .then()
                .statusCode(409)
                .body("code", equalTo("BIZ004"));
    }

    private Long createPost(boolean allowComment) {
        Long postId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(new CreatePostRequest(
                        "댓글 테스트 게시글",
                        "<p>댓글 테스트 본문</p>",
                        "GENERAL",
                        "PUBLISHED",
                        false,
                        allowComment
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
