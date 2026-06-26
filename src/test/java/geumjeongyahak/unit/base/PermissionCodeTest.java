package geumjeongyahak.unit.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geumjeongyahak.domain.base.model.PermissionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionCode 단위 테스트")
class PermissionCodeTest {

    @Test
    @DisplayName("수업 작성 전역 권한은 유효하다")
    void lessonWriteGlobalPermission_isValid() {
        PermissionCode permissionCode = new PermissionCode("lesson:write:*");

        assertThat(permissionCode.value()).isEqualTo("lesson:write:*");
    }

    @Test
    @DisplayName("수업 작성 개별 범위 권한은 허용하지 않는다")
    void lessonWriteTargetPermission_isInvalid() {
        assertThatThrownBy(() -> new PermissionCode("lesson:write:1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("허용되지 않는 권한 범위입니다");
    }
}
