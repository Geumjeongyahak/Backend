package geumjeongyahak.domain.channel.service;

import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.common.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelProxyService {

    private final ChannelAccessChecker channelAccessChecker;

    private final ChannelRepository channelRepository;

    public Channel getActiveById(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted() || !channel.isActive()) {
            throw new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }

        return channel;
    }

    public Channel getActiveDomainLinkedClassroomChannel(Long classroomId) {
        Channel channel = channelRepository.findByChannelTypeAndBindingTypeAndRefIdAndIsDeletedFalse(
                    ChannelType.CLASSROOM,
                    ChannelBindingType.DOMAIN_LINKED,
                    classroomId
                )
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (!channel.isActive()) {
            throw new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }

        return channel;
    }

    /**
     * 다른 도메인이 «이 채널을 읽을 수 있나»만 물을 때 쓴다. 없거나 지운 채널이면 false.
     * 규칙은 {@link ChannelAccessChecker}와 같다.
     */
    @Transactional(readOnly = true)
    public boolean canRead(Long channelId, CustomUserDetails user) {
        try {
            return channelAccessChecker.can("read", channelId, user);
        } catch (ResourceNotFoundException notFound) {
            return false;
        }
    }
}
