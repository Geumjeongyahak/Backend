package geumjeongyahak.unit.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geumjeongyahak.domain.base.service.PermissionRegistryViewService;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionRegistryViewService 단위 테스트")
class PermissionRegistryViewServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @Test
    @DisplayName("전역 범위 권한 코드를 생성한다")
    void buildPermissionCode_WithGlobalScope_ReturnsGlobalPermissionCode() {
        PermissionRegistryViewService service = new PermissionRegistryViewService(channelRepository);

        String permissionCode = service.buildPermissionCode("lesson:write", "*");

        assertThat(permissionCode).isEqualTo("lesson:write:*");
    }

    @Test
    @DisplayName("개별 범위를 지원하지 않는 권한은 개별 범위 코드로 생성하지 않는다")
    void buildPermissionCode_WithUnsupportedTargetScope_ThrowsException() {
        PermissionRegistryViewService service = new PermissionRegistryViewService(channelRepository);

        assertThatThrownBy(() -> service.buildPermissionCode("lesson:write", "lesson:1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("개별 범위를 지원하지 않는 권한 액션입니다");
    }
}
