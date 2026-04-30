package geumjeongyahak.domain.post.service;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.security.service.DomainPermissionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.enums.ChannelWriterPolicy;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.service.UserProxyService;

/**
 * 게시글 작성/수정 권한 검증을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class PostPermissionService {

    private final SubjectProxyService subjectProxyService;
    private final UserProxyService userProxyService;
    private final DomainPermissionChecker domainPermissionChecker;

    @Transactional(readOnly = true)
    public void validateCreatePermission(CustomUserDetails userDetails, Channel channel) {
        // 1. 명시적 권한 확인 (channel:write:* 또는 channel:write:{id})
        if (domainPermissionChecker.hasPermission(userDetails, ResourceType.CHANNEL, ActionType.WRITE, channel.getId())) {
            return;
        }

        // 2. 채널 정책 기반 확인 (기존 로직)
        boolean allowed = switch (channel.getWriterPolicy()) {
            case ALL_AUTHENTICATED -> true;
            case ADMIN_MANAGER_ONLY -> userDetails.isAdminOrManager();
            case CLASSROOM_MANAGER_TEACHER_ONLY -> hasClassroomWritePermission(channel, userDetails.getUserId());
            case DEPARTMENT_MEMBER_OR_ADMIN -> hasDepartmentWritePermission(channel, userDetails.getUserId());
        };

        if (!allowed) {
            throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
        }
    }

    public void validateEditPermission(CustomUserDetails userDetails, Post post) {
        // 1. 관리자 권한 확인
        if (userDetails.isAdminOrManager()) {
            return;
        }

        // 2. 작성자 본인 확인
        if (post.getAuthor().getId().equals(userDetails.getUserId())) {
            return;
        }

        // 3. 명시적 권한 확인 (post:write:* 또는 post:write:{id})
        if (domainPermissionChecker.hasPermission(userDetails, ResourceType.POST, ActionType.WRITE, post.getId())) {
            return;
        }

        throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
    }

    private boolean hasClassroomWritePermission(Channel channel, Long userId) {
        return channel.getWriterPolicy() == ChannelWriterPolicy.CLASSROOM_MANAGER_TEACHER_ONLY
                && channel.getChannelType() == ChannelType.CLASSROOM
                && channel.getRefId() != null
                && subjectProxyService.existsByClassroomIdAndTeacherId(channel.getRefId(), userId);
    }

    private boolean hasDepartmentWritePermission(Channel channel, Long userId) {
        return channel.getWriterPolicy() == ChannelWriterPolicy.DEPARTMENT_MEMBER_OR_ADMIN
                && channel.getChannelType() == ChannelType.DEPARTMENT
                && channel.getRefId() != null
                && userProxyService.existsByIdAndDepartmentId(userId, channel.getRefId());
    }
}
