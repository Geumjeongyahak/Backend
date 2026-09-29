package geumjeongyahak.unit.logging;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.common.logging.RequestLogContextFilter;
import geumjeongyahak.common.security.service.CustomUserDetails;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("RequestLogContextFilter 단위 테스트")
class RequestLogContextFilterTest {

    private final RequestLogContextFilter filter = new RequestLogContextFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("요청 동안 MDC 에 요청·요청 ID·사용자 ID 가 있고, 끝나면 지워진다")
    void putsRequestInfoDuringChainAndClearsAfter() throws Exception {
        CustomUserDetails user = new CustomUserDetails(7L, 70L, "user@test.com", null, null, List.of());
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/admin/purchase-requests/3/approve");
        request.setQueryString("token=secret");
        Map<String, String> duringChain = new HashMap<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> duringChain.putAll(MDC.getCopyOfContextMap()));

        assertThat(duringChain)
            .containsEntry("request", "PATCH /api/v1/admin/purchase-requests/3/approve")
            .containsEntry("user_id", "7")
            .containsKey("trace_id");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("로그인하지 않은 요청은 사용자 ID 없이 남긴다")
    void omitsUserIdWhenAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        Map<String, String> duringChain = new HashMap<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> duringChain.putAll(MDC.getCopyOfContextMap()));

        assertThat(duringChain).containsEntry("request", "POST /api/v1/auth/login").doesNotContainKey("user_id");
    }
}
