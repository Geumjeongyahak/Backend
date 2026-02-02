package sonmoeum.common.security.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Component
public class CookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME)
            .map(this::deserialize)
            .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
        OAuth2AuthorizationRequest authorizationRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        addCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME, serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        // (선택) 프론트에서 redirect_uri를 함께 넘겨줬다면 쿠키로 저장해두기
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return authorizationRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        deleteCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    public Optional<String> getRedirectUriFromCookie(HttpServletRequest request) {
        return getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME).map(Cookie::getValue);
    }

    // Cookie helpers
    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return Optional.of(cookie);
        }
        return Optional.empty();
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // OAuth2 요청정보는 민감하진 않지만, JS 접근 불필요 → HttpOnly
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }


    // (de)serialization
    private String serialize(OAuth2AuthorizationRequest obj) {
        byte[] bytes = SerializationUtils.serialize(obj);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    private OAuth2AuthorizationRequest deserialize(Cookie cookie) {
        byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
        Object obj = SerializationUtils.deserialize(bytes);
        return (OAuth2AuthorizationRequest) obj;
    }
}