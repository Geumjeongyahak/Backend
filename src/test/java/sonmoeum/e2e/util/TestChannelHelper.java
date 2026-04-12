package sonmoeum.e2e.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.repository.ChannelRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestChannelHelper {
    private static final Logger log = LoggerFactory.getLogger(TestChannelHelper.class);

    private final ChannelRepository channelRepository;
    private final Map<Long, Channel> channelCache;

    public TestChannelHelper(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
        this.channelCache = new HashMap<>();
    }

    public void registerChannel(Long channelId) {
        channelRepository.findById(channelId).ifPresentOrElse(
                channel -> channelCache.put(channelId, channel),
                () -> log.warn("채널(ID: {})를 찾을 수 없습니다.", channelId)
        );
    }

    public void clearAll() {
        if (!channelCache.isEmpty()) {
            channelRepository.deleteAll(channelCache.values());
            channelRepository.flush();
            channelCache.clear();
        }
    }
}
