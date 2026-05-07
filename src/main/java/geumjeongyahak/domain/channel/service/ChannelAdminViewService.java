package geumjeongyahak.domain.channel.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.channel.repository.ChannelRepository.AdminChannelProjection;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.domain.channel.v1.dto.request.CreateChannelRequest;
import geumjeongyahak.domain.channel.v1.dto.response.ChannelResponse;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelAdminViewService {

    private final ChannelRepository channelRepository;
    private final ChannelCrudService channelCrudService;

    public AdminPage<AdminChannelRow> getChannels(ChannelFilter filter) {
        List<AdminChannelRow> rows = channelRepository.findAdminChannelsWithoutAllType()
            .stream()
            .filter(channel -> matchesKeyword(channel, filter.keyword()))
            .filter(channel -> isAllOrBlank(filter.channelType()) || channel.getChannelType().equals(filter.channelType()))
            .filter(channel -> isAllOrBlank(filter.bindingType()) || channel.getBindingType().equals(filter.bindingType()))
            .filter(channel -> isAllOrBlank(filter.accessLevel()) || channel.getAccessLevel().equals(filter.accessLevel()))
            .filter(channel -> filter.allowGuestRead() == null || channel.getAllowGuestRead() == filter.allowGuestRead())
            .filter(channel -> filter.isActive() == null || channel.getIsActive() == filter.isActive())
            .filter(channel -> filter.isDefault() == null || channel.getIsDefault() == filter.isDefault())
            .map(AdminChannelRow::from)
            .toList();

        return AdminPage.from(sortChannels(rows, filter.sort()), filter.page(), filter.size());
    }

    public List<ChannelOption> getChannelOptions() {
        return channelRepository.findAdminChannelsWithoutAllType()
            .stream()
            .filter(channel -> Boolean.TRUE.equals(channel.getIsActive()))
            .map(ChannelOption::from)
            .toList();
    }

    public ChannelResponse getChannel(Long channelId) {
        return channelRepository.findById(channelId)
            .filter(channel -> !channel.isDeleted())
            .map(ChannelResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));
    }

    @Transactional
    public Long createChannel(String name, String description, String accessLevel, Boolean allowGuestRead, Boolean isDefault, Boolean isActive) {
        return channelCrudService.createChannel(new CreateChannelRequest(
            name,
            description,
            isDefault,
            isActive,
            accessLevel,
            allowGuestRead
        )).id();
    }

    @Transactional
    public void deleteChannel(Long channelId) {
        channelCrudService.deleteChannel(channelId);
    }

    private List<AdminChannelRow> sortChannels(List<AdminChannelRow> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
            "id", Comparator.comparing(AdminChannelRow::id),
            "name", Comparator.comparing(AdminChannelRow::name, Comparator.nullsLast(String::compareToIgnoreCase)),
            "channelType", Comparator.comparing(AdminChannelRow::channelType, Comparator.nullsLast(String::compareToIgnoreCase)),
            "bindingType", Comparator.comparing(AdminChannelRow::bindingType, Comparator.nullsLast(String::compareToIgnoreCase)),
            "accessLevel", Comparator.comparing(AdminChannelRow::accessLevel, Comparator.nullsLast(String::compareToIgnoreCase)),
            "lastPostedAt", Comparator.comparing(AdminChannelRow::lastPostedAt, Comparator.nullsLast(LocalDateTime::compareTo)),
            "createdAt", Comparator.comparing(AdminChannelRow::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
        ), "createdAt,DESC");
    }

    private boolean matchesKeyword(AdminChannelProjection channel, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(channel.getName(), normalized)
            || contains(channel.getDescription(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAllOrBlank(String value) {
        return isBlank(value) || "ALL".equalsIgnoreCase(value.trim());
    }

    public ChannelType[] getChannelTypes() {
        return ChannelType.values();
    }

    public ChannelBindingType[] getBindingTypes() {
        return ChannelBindingType.values();
    }

    public ChannelAccessLevel[] getAccessLevels() {
        return ChannelAccessLevel.values();
    }

    public record ChannelFilter(
        String keyword,
        String channelType,
        String bindingType,
        String accessLevel,
        Boolean allowGuestRead,
        Boolean isActive,
        Boolean isDefault,
        Integer page,
        Integer size,
        String sort
    ) {
    }

    public record AdminChannelRow(
        Long id,
        String name,
        String description,
        String channelType,
        String bindingType,
        Long refId,
        String accessLevel,
        boolean allowGuestRead,
        boolean isDefault,
        boolean isActive,
        LocalDateTime lastPostedAt,
        LocalDateTime createdAt
    ) {
        private static AdminChannelRow from(AdminChannelProjection channel) {
            return new AdminChannelRow(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getChannelType(),
                channel.getBindingType(),
                channel.getRefId(),
                channel.getAccessLevel(),
                Boolean.TRUE.equals(channel.getAllowGuestRead()),
                Boolean.TRUE.equals(channel.getIsDefault()),
                Boolean.TRUE.equals(channel.getIsActive()),
                channel.getLastPostedAt(),
                channel.getCreatedAt()
            );
        }
    }

    public record ChannelOption(
        Long id,
        String name,
        String channelType
    ) {
        private static ChannelOption from(AdminChannelProjection channel) {
            return new ChannelOption(channel.getId(), channel.getName(), channel.getChannelType());
        }
    }
}
