package sonmoeum.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import sonmoeum.common.security.config.SecurityProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String CLAIM_ROLES = "roles";

    private final SecurityProperties securityProperties;

    private Key signingKey() {
        String secret = securityProperties.getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret(app.jwt.secret)가 설정되어 있지 않습니다.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) { // HS256 권장 최소 32바이트
            throw new IllegalStateException("JWT secret은 최소 32바이트 이상 권장입니다.(현재: " + keyBytes.length + " bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String subject, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwt().getAccessExpSeconds());

        List<String> roles = authorities == null ? List.of() :
            authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        return Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim(CLAIM_ROLES, roles)
            .signWith(signingKey())
            .compact();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token, UserDetails userDetails) {
        // access 토큰에서 꺼낸 권한(roles) 대신 DB 에서 로딩된 userDetails의 권한을 신뢰하는 방식을 사용
        return new UsernamePasswordAuthenticationToken(
            userDetails,  // principal = CustomUserDetails
            null,
            userDetails.getAuthorities()
        );
    }

    public String getSubject(String token) {
        return parse(token).getPayload().getSubject();
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey())
            .build()
            .parseSignedClaims(token);
    }
}
