package sonmoeum.common.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import sonmoeum.common.security.config.SecurityProperties;
import sonmoeum.common.security.jwt.JwtTokenProvider;
import sonmoeum.domain.auth.enums.ProviderType;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final CookieOAuth2AuthorizationRequestRepository authRequestRepository;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {

        // OAuth2User 에서 식별자 추출 (구글 기준 email 사용)
        String subject = extractSubject(authentication);
        if (!StringUtils.hasText(subject)) {
            // 안전하게 실패 처리
            authRequestRepository.removeAuthorizationRequestCookies(request, response);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 로그인 사용자 식별에 실패했습니다.");
            return;
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = toStringOrNull(oAuth2User.getAttributes().get("email"));
        String nameFromGoogle = toStringOrNull(oAuth2User.getAttributes().get("name"));
        String googleSub = toStringOrNull(oAuth2User.getAttributes().get("sub"));

        if (!StringUtils.hasText(email)) {
            // extractSubject가 email 또는 sub를 줄 수 있기 때문에 subject가 email일 가능성도 있음
            email = subject;
        }

        if (!StringUtils.hasText(email)) {
            authRequestRepository.removeAuthorizationRequestCookies(request, response);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 로그인 email 추출에 실패했습니다.");
            return;
        }

        // 이름이 없으면 기본값
        if (!StringUtils.hasText(nameFromGoogle)) {
            nameFromGoogle = "OAuth2 User";
        }

        // sub가 없으면 null로 저장(컬럼 nullable)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userRepository.save(
                User.createOAuthUser(nameFromGoogle, email, ProviderType.GOOGLE, googleSub)
            );
        }

        // email을 subject로 통일
        subject = email;

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        String accessToken = jwtTokenProvider.createAccessToken(subject, authorities);
        String refreshToken = jwtTokenProvider.createRefreshToken(subject);

        // (추후 쿠키 전환 대비) Refresh 토큰은 HttpOnly 쿠키로 내려줌
        addRefreshCookie(response, refreshToken);

        // OAuth2 요청 쿠키 정리
        authRequestRepository.removeAuthorizationRequestCookies(request, response);

        // redirect_uri가 쿠키로 들어있으면 그것을 우선, 없으면 properties 기본값
        String redirectUri = authRequestRepository.getRedirectUriFromCookie(request)
            .filter(StringUtils::hasText)
            .orElse(securityProperties.getOauth2().getRedirectUri());

        // access 토큰 전달 방식(fragment/query)
        String transport = securityProperties.getOauth2().getTokenTransport();
        String target = buildTargetUrl(redirectUri, accessToken, transport);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, target);
    }

    private String extractSubject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) return null;

        // Google: email, sub
        Object email = oAuth2User.getAttributes().get("email");
        if (email != null) return email.toString();

        Object sub = oAuth2User.getAttributes().get("sub");
        if (sub != null) return sub.toString();

        // fallback: name
        Object name = oAuth2User.getAttributes().get("name");
        if (name != null) return name.toString();

        return null;
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        SecurityProperties.Jwt jwt = securityProperties.getJwt();

        long maxAge = jwt.getRefreshCookieMaxAgeSeconds();
        if (maxAge <= 0) {
            maxAge = jwt.getRefreshExpSeconds();
        }

        ResponseCookie cookie = ResponseCookie.from(jwt.getRefreshCookieName(), refreshToken)
            .httpOnly(true)
            .secure(jwt.isRefreshCookieSecure())
            .path(jwt.getRefreshCookiePath())
            .sameSite(jwt.getRefreshCookieSameSite())
            .maxAge(maxAge)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String buildTargetUrl(String base, String accessToken, String transport) {
        String encoded = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        if ("query".equalsIgnoreCase(transport)) {
            String delimiter = base.contains("?") ? "&" : "?";
            return base + delimiter + "access_token=" + encoded;
        }

        // default: fragment
        return base + "#access_token=" + encoded;
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }
}
