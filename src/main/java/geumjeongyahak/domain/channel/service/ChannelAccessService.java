package geumjeongyahak.domain.channel.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.security.service.DomainPermissionChecker;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.comment.entity.Comment;
import geumjeongyahak.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelAccessService {

    private final DomainPermissionChecker permissionChecker;

    /**
     * 채널 목록 필터링 전용. AOP가 적용되지 않는 리스트 컨텍스트에서만 사용한다.
     */
    public boolean canRead(CustomUserDetails userDetails, Channel channel) {
        return userDetails != null && channel.getAccessLevel() != ChannelAccessLevel.CLOSED;
    }

    /**
     * 게시글 수정/삭제 권한 검증.
     * admin/manager, 작성자 본인, 또는 post:manage:{channelId} 보유 시 허용.
     */
    public void validateCanManagePost(CustomUserDetails userDetails, Post post) {
        if (userDetails.isAdminOrManager()) return;
        if (post.getAuthor().getId().equals(userDetails.getUserId())) return;
        if (permissionChecker.hasPermission(userDetails, ResourceType.POST, ActionType.MANAGE, post.getChannel().getId())) return;
        throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
    }

    /**
     * 댓글 수정/삭제 권한 검증.
     * admin/manager, 작성자 본인, 또는 post:manage:{channelId} 보유 시 허용.
     */
    public void validateCanManageComment(CustomUserDetails userDetails, Comment comment) {
        if (userDetails.isAdminOrManager()) return;
        if (comment.getAuthor().getId().equals(userDetails.getUserId())) return;
        if (permissionChecker.hasPermission(userDetails, ResourceType.POST, ActionType.MANAGE, comment.getPost().getChannel().getId())) return;
        throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
    }
}
