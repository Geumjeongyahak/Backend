package geumjeongyahak.domain.auth.external;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import geumjeongyahak.common.config.GoogleOAuth2Properties;
import geumjeongyahak.domain.auth.exception.OAuthProcessingException;
import geumjeongyahak.domain.auth.external.dto.GoogleTokenInfoResponse;
import geumjeongyahak.domain.auth.external.dto.GoogleTokenResponse;
import geumjeongyahak.domain.auth.external.dto.GoogleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleApiClient {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final GoogleOAuth2Properties googleProperties;
    private final RestClient restClient = RestClient.create();

    public GoogleTokenResponse exchangeCode(String code) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", googleProperties.getClientId());
            params.add("client_secret", googleProperties.getClientSecret());
            params.add("code", code);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", googleProperties.getRedirectUri());

            return restClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(GoogleTokenResponse.class);
        } catch (Exception e) {
            log.error("Google 토큰 교환 실패", e);
            throw new OAuthProcessingException("Google 인증 처리 중 오류가 발생했습니다.", e);
        }
    }

    public GoogleUserInfo verifyIdToken(String idToken) {
        try {
            GoogleTokenInfoResponse response = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(GOOGLE_TOKENINFO_URL)
                    .queryParam("id_token", idToken)
                    .build().toUri())
                .retrieve()
                .body(GoogleTokenInfoResponse.class);

            return new GoogleUserInfo(
                response.sub(),
                response.email(),
                "true".equals(response.emailVerified()),
                response.name(),
                response.picture()
            );
        } catch (Exception e) {
            log.error("Google ID Token 검증 실패", e);
            throw new OAuthProcessingException("Google 인증 처리 중 오류가 발생했습니다.", e);
        }
    }
}
