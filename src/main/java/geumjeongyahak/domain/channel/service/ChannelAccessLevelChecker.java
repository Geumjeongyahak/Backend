package geumjeongyahak.domain.channel.service;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.security.service.DomainPermissionChecker;
import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelAccessLevelChecker {

    private final ChannelRepository channelRepository;
    private final DomainPermissionChecker permissionChecker;

    /**
     * 채널 접근 권한 검사.
     *
     * 우선순위:
     * 1. ADMIN 역할 → 무조건 허용
     * 2. permissionCode 계층 확인 (manage > write > read)
     *    - READ_ONLY  minLevel → post:read / post:write / post:manage 중 하나
     *    - READ_WRITE, READ_COMMENT minLevel → post:write / post:manage 중 하나
     * 3. 채널 accessLevel >= minLevel → 허용
     */
    public boolean canAccess(Channel channel, ChannelAccessLevel minLevel, CustomUserDetails userDetails) {
        if (userDetails == null) return false;
        if (userDetails.isAdmin()) return true;
        if (hasPermissionForLevel(userDetails, channel.getId(), minLevel)) return true;
        return channel.getAccessLevel().getPriority() >= minLevel.getPriority();
    }

    /**
     * accessLevel 단순 비교. AOP 외 프로그래매틱 사용 전용.
     */
    public boolean checkAccessLevel(Long channelId, ChannelAccessLevel requiredLevel) {
        return channelRepository.findAccessLevelById(channelId)
                .map(level -> level.getPriority() >= requiredLevel.getPriority())
                .orElse(false);
    }

    private boolean hasPermissionForLevel(CustomUserDetails userDetails, Long channelId, ChannelAccessLevel minLevel) {
        return switch (minLevel) {
            case READ_ONLY -> hasPost(userDetails, channelId, ActionType.READ)
                           || hasPost(userDetails, channelId, ActionType.WRITE)
                           || hasPost(userDetails, channelId, ActionType.MANAGE);
            case READ_COMMENT, READ_WRITE -> hasPost(userDetails, channelId, ActionType.WRITE)
                                          || hasPost(userDetails, channelId, ActionType.MANAGE);
            default -> hasPost(userDetails, channelId, ActionType.MANAGE);
        };
    }

    private boolean hasPost(CustomUserDetails userDetails, Long channelId, ActionType action) {
        return permissionChecker.hasPermission(userDetails, ResourceType.POST, action, channelId);
    }
}
