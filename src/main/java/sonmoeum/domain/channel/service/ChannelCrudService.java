package sonmoeum.domain.channel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.CommonErrorCode;
import sonmoeum.common.exception.DuplicateResourceException;
import sonmoeum.common.exception.ResourceNotFoundException;
import sonmoeum.domain.channel.exception.ChannelErrorCode;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.channel.enums.ChannelWriterPolicy;
import sonmoeum.domain.channel.repository.ChannelRepository;
import sonmoeum.domain.channel.repository.ChannelSpecs;
import sonmoeum.domain.channel.v1.dto.request.ChannelListRequest;
import sonmoeum.domain.channel.v1.dto.request.CreateChannelRequest;
import sonmoeum.domain.channel.v1.dto.request.UpdateChannelRequest;
import sonmoeum.domain.channel.v1.dto.response.ChannelResponse;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelCrudService {
    private final ChannelRepository channelRepository;
    private final ChannelRefIdResolver channelRefIdResolver;

    @Transactional
    public ChannelResponse createChannel(CreateChannelRequest request) {
        log.debug("채널 생성 시도: slug={}", request.slug());

        validateDuplicateSlug(request.slug(), null);
        ChannelType channelType = parseChannelType(request.channelType());

        Channel channel = channelRepository.save(Channel.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .channelType(channelType)
                .refId(channelRefIdResolver.resolve(
                        channelType,
                        request.classroomId(),
                        request.departmentId(),
                        request.customRefId()
                ))
                .writerPolicy(resolveWriterPolicy(request.writerPolicy()))
                .isDefault(request.isDefault())
                .isActive(request.isActive())
                .sortOrder(request.sortOrder())
                .build());

        log.info("채널 생성 성공: id={}, slug={}", channel.getId(), channel.getSlug());
        return ChannelResponse.from(channel);
    }

    public ChannelResponse getChannel(Long id) {
        return ChannelResponse.from(getChannelWithoutDeleted(id));
    }

    public List<ChannelResponse> getChannels(ChannelListRequest request) {
        log.debug("채널 목록 조회 시도: type={}, active={}", request.getChannelType(), request.getIsActive());

        Specification<Channel> spec = ChannelSpecs.withoutDeleted();

        if (request.getName() != null && !request.getName().isBlank()) {
            spec = spec.and(ChannelSpecs.containsName(request.getName()));
        }
        if (request.getChannelType() != null && !request.getChannelType().isBlank()) {
            spec = spec.and(ChannelSpecs.hasChannelType(parseChannelType(request.getChannelType())));
        }
        spec = spec.and(ChannelSpecs.hasIsActive(request.getIsActive() != null ? request.getIsActive() : true));
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
                .map(ChannelResponse::from)
                .toList();
    }

    @Transactional
    public ChannelResponse updateChannel(Long id, UpdateChannelRequest request) {
        log.debug("채널 수정 시도: id={}", id);

        Channel channel = getActiveChannel(id);
        boolean isUpdated = false;

        if (request.name() != null) {
            channel.setName(request.name());
            isUpdated = true;
        }
        if (request.slug() != null) {
            validateDuplicateSlug(request.slug(), id);
            channel.setSlug(request.slug());
            isUpdated = true;
        }
        if (request.description() != null) {
            channel.setDescription(request.description());
            isUpdated = true;
        }
        if (request.channelType() != null) {
            channel.setChannelType(parseChannelType(request.channelType()));
            isUpdated = true;
        }
        if (request.writerPolicy() != null) {
            channel.setWriterPolicy(resolveWriterPolicy(request.writerPolicy()));
            isUpdated = true;
        }
        if (request.channelType() != null
                || request.classroomId() != null
                || request.departmentId() != null
                || request.customRefId() != null) {
            ChannelType effectiveType = request.channelType() != null
                    ? parseChannelType(request.channelType())
                    : channel.getChannelType();
            channel.setRefId(channelRefIdResolver.resolveForUpdate(
                    channel,
                    effectiveType,
                    request.classroomId(),
                    request.departmentId(),
                    request.customRefId()
            ));
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
        if (request.sortOrder() != null) {
            channel.setSortOrder(request.sortOrder());
            isUpdated = true;
        }

        if (!isUpdated) {
            throw new BusinessException(CommonErrorCode.NO_CHANGES_DETECTED);
        }

        Channel updated = channelRepository.save(channel);
        log.info("채널 수정 성공: id={}", updated.getId());
        return ChannelResponse.from(updated);
    }

    @Transactional
    public ChannelResponse hideChannel(Long id) {
        log.debug("채널 숨김 시도: id={}", id);
        Channel channel = getActiveChannel(id);
        channel.setActive(false);
        return ChannelResponse.from(channelRepository.save(channel));
    }

    @Transactional
    public ChannelResponse showChannel(Long id) {
        log.debug("채널 표시 시도: id={}", id);
        Channel channel = getActiveChannel(id);
        channel.setActive(true);
        return ChannelResponse.from(channelRepository.save(channel));
    }

    @Transactional
    public void deleteChannel(Long id) {
        log.debug("채널 삭제 시도: id={}", id);
        Channel channel = getActiveChannel(id);
        channel.setDeleted(true);
        channel.setActive(false);
        channelRepository.save(channel);
        log.info("채널 삭제 성공: id={}", id);
    }

    @Transactional
    public void updateLastPostedAt(Long channelId, LocalDateTime postedAt) {
        Channel channel = getActiveChannel(channelId);
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

    private Channel getActiveChannel(Long id) {
        Channel channel = getChannelWithoutDeleted(id);
        return channel;
    }

    private void validateDuplicateSlug(String slug, Long id) {
        boolean isDuplicate = id == null
                ? channelRepository.existsBySlugAndIsDeletedFalse(slug)
                : channelRepository.existsBySlugAndIdNotAndIsDeletedFalse(slug, id);

        if (isDuplicate) {
            throw new DuplicateResourceException(ChannelErrorCode.DUPLICATE_CHANNEL);
        }
    }

    private ChannelWriterPolicy resolveWriterPolicy(String writerPolicy) {
        if (writerPolicy == null) {
            return ChannelWriterPolicy.ALL_AUTHENTICATED;
        }

        try {
            return ChannelWriterPolicy.valueOf(writerPolicy);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 채널 작성 권한 정책입니다.");
        }
    }

    private ChannelType parseChannelType(String channelType) {
        try {
            return ChannelType.valueOf(channelType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 채널 유형입니다.");
        }
    }
}
