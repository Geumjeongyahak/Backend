package sonmoeum.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.domain.auth.exception.AuthErrorCode;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.channel.enums.ChannelWriterPolicy;
import sonmoeum.domain.department.service.UserDepartmentProxyService;
import sonmoeum.domain.post.entity.Post;
import sonmoeum.domain.subject.service.SubjectProxyService;

/**
 * 게시글 작성/수정 권한 검증을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class PostPermissionService {

    private final SubjectProxyService subjectProxyService;
    private final UserDepartmentProxyService userDepartmentProxyService;

    @Transactional(readOnly = true)
    public void validateCreatePermission(Long userId, boolean isAdminOrManager, Channel channel) {
        if (isAdminOrManager) {
            return;
        }

        boolean allowed = switch (channel.getWriterPolicy()) {
            case ALL_AUTHENTICATED -> true;
            case ADMIN_MANAGER_ONLY -> false;
            case CLASSROOM_MANAGER_TEACHER_ONLY -> hasClassroomWritePermission(channel, userId);
            case DEPARTMENT_MEMBER_OR_ADMIN -> hasDepartmentWritePermission(channel, userId);
        };

        if (!allowed) {
            throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
        }
    }

    public void validateEditPermission(Long userId, boolean isAdminOrManager, Post post) {
        if (isAdminOrManager || post.getAuthor().getId().equals(userId)) {
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
                && userDepartmentProxyService.existsByUserIdAndDepartmentId(userId, channel.getRefId());
    }
}
