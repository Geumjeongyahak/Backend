package geumjeongyahak.domain.channel.service;

import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.security.service.DomainPermissionChecker;
import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("channelAccess")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelAccessChecker {

    private final ChannelRepository channelRepository;
    private final DomainPermissionChecker permissionChecker;

    /**
     * 액션, 채널 ID 기반 통합 권한 검사 (SPeL 사용용)
     * 모든 명시적 권한은 ResourceType.CHANNEL 기준으로 체크한다. (예: channel:manage:1)
     */
    public boolean can(String actionCode, Long channelId, CustomUserDetails userDetails) {
        if (channelId == null) return false;
        if (userDetails != null && userDetails.isAdmin()) return true;

        ActionType action = ActionType.fromCode(actionCode);

        // 1. 명시적 채널 권한 확인 (인증된 사용자만 가능)
        if (userDetails != null && permissionChecker.hasPermission(userDetails, ResourceType.CHANNEL, action, channelId)) return true;

        // 2. 채널 정책 확인
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted()) {
            throw new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }

        return can(channel, action, userDetails);
    }

    /**
     * 엔티티 기반 권한 검사
     */
    public boolean can(Channel channel, ActionType action, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.isAdmin()) return true;

        // 1. 명시적 권한 확인
        if (userDetails != null && permissionChecker.hasPermission(userDetails, ResourceType.CHANNEL, action, channel.getId())) return true;

        // 2. 일반 사용자/GUEST 정책 확인
        if (!channel.isActive()) return false;

        // Guest Read가 허용된 경우 조회 액션은 무조건 허용 (최우선 순위)
        if (action == ActionType.READ && channel.isAllowGuestRead()) {
            return true;
        }

        // GUEST 접근 제어: 유저가 없으면 위에서 처리되지 않은 경우(조회 아님 등) 거부
        if (userDetails == null) {
            return false;
        }

        return isPolicySatisfied(channel.getAccessLevel(), action);
    }

    private boolean isPolicySatisfied(ChannelAccessLevel level, ActionType action) {
        return switch (action) {
            case READ -> level != ChannelAccessLevel.CLOSED;
            case WRITE -> level.getPriority() >= ChannelAccessLevel.READ_WRITE.getPriority();
            case MANAGE -> false; // 관리 권한은 정책만으로 허용 안됨 (ADMIN 또는 명시적 권한 필요)
            default -> false;
        };
    }

    /**
     * 댓글 작성을 위한 별도 체크 (READ_COMMENT 레벨 허용)
     */
    public boolean canWriteComment(Long channelId, CustomUserDetails userDetails) {
        if (channelId == null || userDetails == null) return false;
        if (userDetails.isAdmin()) return true;

        // 명시적 권한 (channel:write or channel:manage)
        if (permissionChecker.hasPermission(userDetails, ResourceType.CHANNEL, ActionType.WRITE, channelId) ||
            permissionChecker.hasPermission(userDetails, ResourceType.CHANNEL, ActionType.MANAGE, channelId)) return true;

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted() || !channel.isActive()) return false;

        return channel.getAccessLevel().getPriority() >= ChannelAccessLevel.READ_COMMENT.getPriority();
    }
}
