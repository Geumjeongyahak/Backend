package geumjeongyahak.domain.channel.service;

import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelProxyService {

    private final ChannelRepository channelRepository;

    public Channel getActiveById(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted() || !channel.isActive()) {
            throw new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }

        return channel;
    }
}
