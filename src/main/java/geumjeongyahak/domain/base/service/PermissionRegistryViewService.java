package geumjeongyahak.domain.base.service;

import java.util.List;
import java.util.stream.Stream;

import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.base.enums.ResourceType;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.base.model.PermissionDefinition;
import geumjeongyahak.domain.base.model.PermissionRegistry;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionRegistryViewService {

    private final ChannelRepository channelRepository;

    public List<PermissionDefinition> getGlobalPermissions() {
        return PermissionRegistry.getGlobalPermissions();
    }

    public List<PermissionDefinition> getAssignablePermissions() {
        return PermissionRegistry.getAssignablePermissions();
    }

    public List<PermissionScopeOption> getAssignableScopes() {
        return Stream.concat(
            Stream.of(PermissionScopeOption.global()),
            getChannelScopeOptions().stream()
        ).toList();
    }

    public String buildPermissionCode(String permissionKey, String scopeTarget) {
        String[] parts = permissionKey == null ? new String[0] : permissionKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("유효하지 않은 권한 선택입니다: " + permissionKey);
        }

        ResourceType resource = ResourceType.fromCode(parts[0]);
        ActionType action = ActionType.fromCode(parts[1]);
        if ("*".equals(scopeTarget)) {
            return PermissionCode.global(resource, action).value();
        }

        PermissionScopeSelection scope = PermissionScopeSelection.parse(scopeTarget);
        if (scope.resource() != resource) {
            throw new IllegalArgumentException("권한과 범위의 리소스가 일치하지 않습니다.");
        }
        if (!PermissionRegistry.isScopeAllowed(resource, action, false)) {
            throw new IllegalArgumentException("개별 범위를 지원하지 않는 권한 액션입니다: " + permissionKey);
        }
        return PermissionCode.of(resource, action, scope.targetId()).value();
    }

    private List<PermissionScopeOption> getChannelScopeOptions() {
        return channelRepository.findAdminChannelsWithoutAllType()
            .stream()
            .filter(channel -> Boolean.TRUE.equals(channel.getIsActive()))
            .map(channel -> new PermissionScopeOption(
                ResourceType.CHANNEL.getCode() + ":" + channel.getId(),
                channelScopeLabel(channel.getChannelType(), channel.getName()),
                ResourceType.CHANNEL.getCode(),
                channel.getId()
            ))
            .toList();
    }

    private String channelScopeLabel(String channelType, String channelName) {
        String prefix = switch (channelType) {
            case "DEPARTMENT" -> "부서 채널";
            case "CLASSROOM" -> "분반 채널";
            case "NOTICE" -> "공지 채널";
            case "EVENT" -> "행사 채널";
            case "RESOURCE" -> "자료 채널";
            default -> "채널";
        };
        return prefix + ": " + channelName;
    }

    public record PermissionScopeOption(
        String value,
        String label,
        String resourceCode,
        Long targetId
    ) {
        private static PermissionScopeOption global() {
            return new PermissionScopeOption("*", "전체", "*", null);
        }
    }

    private record PermissionScopeSelection(ResourceType resource, Long targetId) {
        private static PermissionScopeSelection parse(String value) {
            String[] parts = value == null ? new String[0] : value.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("유효하지 않은 권한 범위입니다: " + value);
            }

            try {
                return new PermissionScopeSelection(ResourceType.fromCode(parts[0]), Long.parseLong(parts[1]));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("유효하지 않은 권한 범위 ID입니다: " + value);
            }
        }
    }
}
