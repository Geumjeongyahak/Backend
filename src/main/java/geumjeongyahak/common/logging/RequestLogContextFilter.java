package geumjeongyahak.common.logging;

import geumjeongyahak.common.security.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 하나 동안 찍히는 모든 로그에 요청 ID, 메서드·경로, 사용자 ID 를 붙인다.
 * 사용자 ID 를 알려면 JWT 필터 뒤에서 돌아야 한다 (WebSecurityConfig).
 */
@Component
public class RequestLogContextFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "trace_id";
    private static final String REQUEST = "request";
    private static final String USER_ID = "user_id";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        MDC.put(TRACE_ID, UUID.randomUUID().toString());
        // 쿼리 문자열은 남기지 않는다. 토큰이 실리는 경로가 있다.
        MDC.put(REQUEST, request.getMethod() + " " + request.getRequestURI());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            MDC.put(USER_ID, String.valueOf(user.getUserId()));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(REQUEST);
            MDC.remove(USER_ID);
        }
    }
}
