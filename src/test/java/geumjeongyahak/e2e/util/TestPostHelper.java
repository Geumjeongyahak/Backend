package geumjeongyahak.e2e.util;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.post.repository.PostRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestPostHelper {
    private static final Logger log = LoggerFactory.getLogger(TestPostHelper.class);

    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final Map<Long, Post> postCache;

    public TestPostHelper(
            PostRepository postRepository,
            PostFileRepository postFileRepository,
            PostAttachmentRepository postAttachmentRepository) {
        this.postRepository = postRepository;
        this.postFileRepository = postFileRepository;
        this.postAttachmentRepository = postAttachmentRepository;
        this.postCache = new HashMap<>();
    }

    public void registerPost(Long postId) {
        postRepository.findById(postId).ifPresentOrElse(
                post -> postCache.put(postId, post),
                () -> log.warn("게시글(ID: {})을 찾을 수 없습니다.", postId)
        );
    }

    public Long createDraftAndRegister(Long channelId, String accessToken) {
        Long postId = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/channels/{channelId}/posts/drafts", channelId)
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        registerPost(postId);
        return postId;
    }

    public void clearAll() {
        if (!postCache.isEmpty()) {
            postAttachmentRepository.deleteAll();
            postFileRepository.deleteAll();
            postRepository.deleteAll(postCache.values());
            postRepository.flush();
            postCache.clear();
        }
    }
}
