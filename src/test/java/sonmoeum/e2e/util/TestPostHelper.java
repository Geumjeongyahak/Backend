package sonmoeum.e2e.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.post.entity.Post;
import sonmoeum.domain.post.repository.PostRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestPostHelper {
    private static final Logger log = LoggerFactory.getLogger(TestPostHelper.class);

    private final PostRepository postRepository;
    private final Map<Long, Post> postCache;

    public TestPostHelper(PostRepository postRepository) {
        this.postRepository = postRepository;
        this.postCache = new HashMap<>();
    }

    public void registerPost(Long postId) {
        postRepository.findById(postId).ifPresentOrElse(
                post -> postCache.put(postId, post),
                () -> log.warn("게시글(ID: {})을 찾을 수 없습니다.", postId)
        );
    }

    public void clearAll() {
        if (!postCache.isEmpty()) {
            postRepository.deleteAll(postCache.values());
            postRepository.flush();
            postCache.clear();
        }
    }
}
