package geumjeongyahak.domain.post.service;

import org.springframework.stereotype.Component;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;

@Component("postAccess")
@RequiredArgsConstructor
public class PostAccessChecker {
    private final PostRepository postRepository;

    public boolean can(Long postId, CustomUserDetails userDetail) {
        if (postId == null || userDetail == null) return false;

        return postRepository.findAuthorIdById(postId)
                .map(authorId -> authorId.equals(userDetail.getUserId()))
                .orElse(false);
    }
}
