package geumjeongyahak.domain.channel.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.base.enums.ActionType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.domain.channel.repository.ChannelSpecs;
import geumjeongyahak.domain.channel.v1.dto.request.ChannelListRequest;
import geumjeongyahak.domain.channel.v1.dto.request.CreateChannelRequest;
import geumjeongyahak.domain.channel.v1.dto.request.UpdateChannelRequest;
import geumjeongyahak.domain.channel.v1.dto.response.ChannelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelCrudService {

    private static final Set<ChannelType> MANUALLY_CREATABLE_CHANNEL_TYPES = EnumSet.of(
            ChannelType.NOTICE,
            ChannelType.EVENT,
            ChannelType.RESOURCE,
            ChannelType.GUIDE,
            ChannelType.CUSTOM
    );

    private final ChannelRepository channelRepository;
    private final ChannelAccessChecker channelAccess;

    @Transactional
    public ChannelResponse createChannel(CreateChannelRequest request) {
        ChannelType channelType = resolveCreateChannelType(request.channelType());
        log.debug("수동 채널 생성 요청: name={}, channelType={}", request.name(), channelType);

        Channel channel = channelRepository.save(Channel.builder()
                .name(request.name())
                .description(request.description())
                .channelType(channelType)
                .bindingType(ChannelBindingType.STANDALONE)
                .refId(null)
                .accessLevel(ChannelAccessLevel.valueOf(request.accessLevel()))
                .allowGuestRead(request.allowGuestRead())
                .isDefault(request.isDefault())
                .isActive(request.isActive())
                .build());

        log.info("수동 채널 생성 성공: id={}, channelType={}", channel.getId(), channelType);
        return ChannelResponse.from(channel);
    }

    public ChannelResponse getChannel(Long id, CustomUserDetails userDetails) {
        Channel channel = getChannelWithoutDeleted(id);
        return ChannelResponse.from(channel);
    }

    public List<ChannelResponse> getChannels(ChannelListRequest request, CustomUserDetails userDetails) {
        Specification<Channel> spec = ChannelSpecs.withoutDeleted();

        if (request.getName() != null && !request.getName().isBlank()) {
            spec = spec.and(ChannelSpecs.containsName(request.getName()));
        }
        if (request.getChannelType() != null && !request.getChannelType().isBlank()
                && !"ALL".equalsIgnoreCase(request.getChannelType())) {
            spec = spec.and(ChannelSpecs.hasChannelType(ChannelType.valueOf(request.getChannelType())));
        }
        if (request.getBindingType() != null && !request.getBindingType().isBlank()
                && !"ALL".equalsIgnoreCase(request.getBindingType())) {
            spec = spec.and(ChannelSpecs.hasBindingType(ChannelBindingType.valueOf(request.getBindingType())));
        }
        if (request.getIsActive() != null) {
            spec = spec.and(ChannelSpecs.hasIsActive(request.getIsActive()));
        }
        if (request.getIsDefault() != null) {
            spec = spec.and(ChannelSpecs.hasIsDefault(request.getIsDefault()));
        }
        if (request.getClassroomId() != null) {
            spec = spec.and(ChannelSpecs.hasChannelType(ChannelType.CLASSROOM))
                    .and(ChannelSpecs.hasRefId(request.getClassroomId()));
        }
        if (request.getDepartmentId() != null) {
            spec = spec.and(ChannelSpecs.hasChannelType(ChannelType.DEPARTMENT))
                    .and(ChannelSpecs.hasRefId(request.getDepartmentId()));
        }

        return channelRepository.findAll(spec, request.toSort()).stream()
                .filter(channel -> channelAccess.can(channel, ActionType.READ, userDetails))
                .map(ChannelResponse::from)
                .toList();
    }

    @Transactional
    public ChannelResponse updateChannel(Long id, UpdateChannelRequest request) {
        Channel channel = getStandaloneChannel(id);
        boolean isUpdated = false;

        if (request.name() != null) {
            channel.setName(request.name());
            isUpdated = true;
        }
        if (request.description() != null) {
            channel.setDescription(request.description());
            isUpdated = true;
        }
        if (request.accessLevel() != null) {
            channel.setAccessLevel(ChannelAccessLevel.valueOf(request.accessLevel()));
            isUpdated = true;
        }
        if (request.allowGuestRead() != null) {
            channel.setAllowGuestRead(request.allowGuestRead());
            isUpdated = true;
        }
        if (request.isDefault() != null) {
            channel.setDefault(request.isDefault());
            isUpdated = true;
        }
        if (request.isActive() != null) {
            channel.setActive(request.isActive());
            isUpdated = true;
        }

        if (!isUpdated) {
            throw new BusinessException(CommonErrorCode.NO_CHANGES_DETECTED);
        }

        log.info("커스텀 채널 수정 성공: id={}", channel.getId());
        return ChannelResponse.from(channelRepository.save(channel));
    }

    @Transactional
    public void deleteChannel(Long id) {
        Channel channel = getStandaloneChannel(id);
        channel.setDeleted(true);
        channel.setActive(false);
        channelRepository.save(channel);
        log.info("커스텀 채널 삭제 성공: id={}", id);
    }

    @Transactional
    public void updateLastPostedAt(Long channelId, LocalDateTime postedAt) {
        Channel channel = getChannelWithoutDeleted(channelId);
        channel.setLastPostedAt(postedAt);
        channelRepository.save(channel);
    }

    private Channel getChannelWithoutDeleted(Long id) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted()) {
            throw new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }

        return channel;
    }

    private ChannelType resolveCreateChannelType(String channelType) {
        if (channelType == null || channelType.isBlank()) {
            return ChannelType.CUSTOM;
        }

        ChannelType resolved;
        try {
            resolved = ChannelType.valueOf(channelType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT,
                    "알 수 없는 채널 유형입니다. 허용 값: NOTICE, EVENT, RESOURCE, GUIDE, CUSTOM"
            );
        }

        if (!MANUALLY_CREATABLE_CHANNEL_TYPES.contains(resolved)) {
            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT,
                    "수동 생성 채널 유형은 NOTICE, EVENT, RESOURCE, GUIDE, CUSTOM만 허용됩니다."
            );
        }
        return resolved;
    }

    private Channel getStandaloneChannel(Long id) {
        Channel channel = getChannelWithoutDeleted(id);
        if (channel.getBindingType() != ChannelBindingType.STANDALONE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "도메인 연동 채널은 일반 채널 관리 API로 수정하거나 삭제할 수 없습니다.");
        }
        return channel;
    }
}
