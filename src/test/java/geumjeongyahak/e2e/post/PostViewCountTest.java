package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.repository.PostRepository;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 게시글 조회수 테스트")
class PostViewCountTest extends BasePostTest {

    private static final int CONCURRENT_VIEWS = 20;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("같은 게시글을 동시에 열면 연 횟수만큼 조회수가 오른다")
    void getPost_concurrently_countsEveryView() throws Exception {
        Long postId = createViewCountPost("동시 조회수 공지");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_VIEWS);
        try {
            List<Future<Integer>> views = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_VIEWS; i++) {
                views.add(executor.submit(() -> {
                    start.await();
                    return getPostStatus(postId);
                }));
            }
            start.countDown();
            for (Future<Integer> view : views) {
                assertThat(view.get(30, TimeUnit.SECONDS)).isEqualTo(200);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(postRepository.findById(postId).orElseThrow().getViewCount()).isEqualTo(CONCURRENT_VIEWS);
    }

    @Test
    @DisplayName("게시글을 열어도 수정 시각은 바뀌지 않고, 응답에는 이번 조회가 더해진 조회수가 실린다")
    void getPost_doesNotTouchUpdatedAt() {
        Long postId = createViewCountPost("수정 시각 공지");
        Post before = postRepository.findById(postId).orElseThrow();

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
                .then()
                .statusCode(200)
                .body("viewCount", equalTo(1));

        Post after = postRepository.findById(postId).orElseThrow();
        assertThat(after.getViewCount()).isEqualTo(1);
        assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
    }

    private int getPostStatus(Long postId) {
        return given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
                .then()
                .extract()
                .statusCode();
    }

    private Long createViewCountPost(String title) {
        CreatePostRequest request = new CreatePostRequest(title, "<p>" + title + "</p>", "PUBLISHED", false, true, null);
        Long postId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/channels/{channelId}/posts", noticeChannelId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getLong("id");
        testPostHelper.registerPost(postId);
        return postId;
    }
}
