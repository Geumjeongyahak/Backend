package geumjeongyahak.unit.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.domain.channel.service.SystemChannelService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private SystemChannelService systemChannelService;

    @Test
    void ensureNoticeChannel_createsGuestReadableChannel() {
        when(channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(eq(ChannelType.NOTICE), isNull()))
                .thenReturn(Optional.empty());
        when(channelRepository.save(any(Channel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Channel channel = systemChannelService.ensureNoticeChannel("공지사항", "기관 공지", true);

        assertThat(channel.isAllowGuestRead()).isTrue();
    }

    @Test
    void ensureEventChannel_createsGuestReadableChannel() {
        when(channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(eq(ChannelType.EVENT), isNull()))
                .thenReturn(Optional.empty());
        when(channelRepository.save(any(Channel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Channel channel = systemChannelService.ensureEventChannel("행사안내", "기관 행사", true);

        assertThat(channel.isAllowGuestRead()).isTrue();
    }

    @Test
    void ensureResourceChannel_createsAuthenticatedReadChannel() {
        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
        when(channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(eq(ChannelType.RESOURCE), isNull()))
                .thenReturn(Optional.empty());
        when(channelRepository.save(channelCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Channel channel = systemChannelService.ensureResourceChannel("자료실", "기관 자료", true);

        assertThat(channel.isAllowGuestRead()).isFalse();
        assertThat(channelCaptor.getValue().isAllowGuestRead()).isFalse();
    }
}
