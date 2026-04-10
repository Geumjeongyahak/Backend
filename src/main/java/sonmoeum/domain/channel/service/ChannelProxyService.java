package sonmoeum.domain.channel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.repository.ChannelRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted() || !channel.isActive()) {
            throw new ResourceNotFoundException(ErrorCode.CHANNEL_NOT_FOUND);
        }

        return channel;
    }
}
