package sonmoeum.domain.channel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.classroom.service.ClassroomProxyService;
import sonmoeum.domain.department.service.DepartmentProxyService;

/**
 * 채널 유형별 참조 ID 규칙을 해석하고 검증한다.
 */
@Component
@RequiredArgsConstructor
public class ChannelRefIdResolver {

    private final ClassroomProxyService classroomProxyService;
    private final DepartmentProxyService departmentProxyService;

    public Long resolve(ChannelType channelType, Long classroomId, Long departmentId, Long customRefId) {
        return switch (channelType) {
            case ALL -> {
                validateNoRefId(classroomId, departmentId, customRefId);
                yield null;
            }
            case CLASSROOM -> requireExistingClassroomId(classroomId, departmentId, customRefId);
            case DEPARTMENT -> requireExistingDepartmentId(classroomId, departmentId, customRefId);
            case CUSTOM -> requireCustomRefId(classroomId, departmentId, customRefId);
        };
    }

    public Long resolveForUpdate(
            Channel channel,
            ChannelType targetType,
            Long classroomId,
            Long departmentId,
            Long customRefId
    ) {
        return resolve(
                targetType,
                classroomId != null ? classroomId : getClassroomId(channel),
                departmentId != null ? departmentId : getDepartmentId(channel),
                customRefId != null ? customRefId : getCustomRefId(channel)
        );
    }

    private Long getClassroomId(Channel channel) {
        return channel.getChannelType() == ChannelType.CLASSROOM ? channel.getRefId() : null;
    }

    private Long getDepartmentId(Channel channel) {
        return channel.getChannelType() == ChannelType.DEPARTMENT ? channel.getRefId() : null;
    }

    private Long getCustomRefId(Channel channel) {
        return channel.getChannelType() == ChannelType.CUSTOM ? channel.getRefId() : null;
    }

    private void validateNoRefId(Long classroomId, Long departmentId, Long customRefId) {
        if (classroomId != null || departmentId != null || customRefId != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ALL 채널에는 참조 ID를 지정할 수 없습니다.");
        }
    }

    private Long requireExistingClassroomId(Long classroomId, Long departmentId, Long customRefId) {
        if (classroomId == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "CLASSROOM 채널에는 classroomId가 필요합니다.");
        }
        if (departmentId != null || customRefId != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "CLASSROOM 채널에는 classroomId만 지정할 수 있습니다.");
        }

        classroomProxyService.getActiveById(classroomId);
        return classroomId;
    }

    private Long requireExistingDepartmentId(Long classroomId, Long departmentId, Long customRefId) {
        if (departmentId == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "DEPARTMENT 채널에는 departmentId가 필요합니다.");
        }
        if (classroomId != null || customRefId != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "DEPARTMENT 채널에는 departmentId만 지정할 수 있습니다.");
        }

        departmentProxyService.getById(departmentId);
        return departmentId;
    }

    private Long requireCustomRefId(Long classroomId, Long departmentId, Long customRefId) {
        if (customRefId == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "CUSTOM 채널에는 customRefId가 필요합니다.");
        }
        if (classroomId != null || departmentId != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "CUSTOM 채널에는 customRefId만 지정할 수 있습니다.");
        }
        return customRefId;
    }
}
