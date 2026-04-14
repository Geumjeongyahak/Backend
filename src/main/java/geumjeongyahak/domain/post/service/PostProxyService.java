package geumjeongyahak.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.exception.PostErrorCode;
import geumjeongyahak.domain.post.repository.PostRepository;

/**
 * Post 도메인의 Proxy Service.
 * 다른 도메인에서 게시글 참조 조회가 필요할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class PostProxyService {

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public Post getActiveByChannelId(Long channelId, Long postId) {
        Post post = postRepository.findByIdAndChannelId(postId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted() || !post.belongsTo(channelId)) {
            throw new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND);
        }

        return post;
    }
}
