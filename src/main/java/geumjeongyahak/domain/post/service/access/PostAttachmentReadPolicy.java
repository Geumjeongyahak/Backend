package geumjeongyahak.domain.post.service.access;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.channel.service.ChannelAccessChecker;
import geumjeongyahak.domain.file.service.access.AttachmentReadPolicy;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 첨부·이미지: 게시된 글이면 그 채널을 읽을 수 있는 사람, 그 밖(임시저장 등)이면 작성자만.
 * 게시글 열람 규칙(채널 {@code read}, 임시저장은 {@code PostAccessChecker})과 같아야 한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostAttachmentReadPolicy implements AttachmentReadPolicy {

    private final PostRepository postRepository;
    private final ChannelAccessChecker channelAccessChecker;

    @Override
    public boolean canRead(UUID fileId, CustomUserDetails user) {
        return postRepository.findFileHoldersByFileId(fileId).stream().anyMatch(post ->
            post.getStatus() == PostStatus.PUBLISHED
                ? channelAccessChecker.can("read", post.getChannelId(), user)
                : user != null && post.getAuthorId().equals(user.getUserId()));
    }
}
