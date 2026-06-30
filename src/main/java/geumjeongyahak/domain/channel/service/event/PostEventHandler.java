package geumjeongyahak.domain.channel.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import geumjeongyahak.domain.channel.service.ChannelCrudService;
import geumjeongyahak.domain.post.event.PostChangedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostEventHandler {

    private final ChannelCrudService channelCrudService;

    @TransactionalEventListener
    public void handlePostChanged(PostChangedEvent event) {
        log.info("게시글 변경 이벤트 처리 - 채널 최근 게시 시각 갱신 (channelId={})", event.getChannelId());
        channelCrudService.updateLastPostedAt(event.getChannelId(), event.getLastPostedAt());
    }
}
