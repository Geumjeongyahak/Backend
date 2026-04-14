package geumjeongyahak.domain.channel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.repository.ChannelRepository;

/**
 * Channel 도메인의 Proxy Service.
 * 다른 도메인에서 채널 참조 조회가 필요할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class ChannelProxyService {

    private final ChannelRepository channelRepository;

    @Transactional(readOnly = true)
    public Channel getReadableById(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted() || !channel.isActive()) {
            throw new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }

        return channel;
    }
}
