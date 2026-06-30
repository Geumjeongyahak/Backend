package geumjeongyahak.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import geumjeongyahak.common.security.config.SecurityProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
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

    public String createAccessToken(String subject) {
        return createToken(subject, securityProperties.getJwt().getAccessExpSeconds());
    }

    public String createOAuth2TempToken(String subject, String email, String profileImageUrl) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwt().getOauth2TempExpSeconds());
        return Jwts.builder()
            .subject(subject)
            .claim("email", email)
            .claim("profileImageUrl", profileImageUrl)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(signingKey())
            .compact();
    }

    public String createToken(String subject, long expSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expSeconds);
        return Jwts.builder()
            .subject(subject)
            .issuer("geumjeongyahak")
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(signingKey())
            .compact();
    }

    public String createToken(String subject, long expSeconds, String issuer) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expSeconds);
        return Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
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

    public String getSubject(String token) {
        return parse(token).getPayload().getSubject();
    }

    public String getEmail(String token) {
        return parse(token).getPayload().get("email", String.class);
    }

    public String getProfileImageUrl(String token) {
        return parse(token).getPayload().get("profileImageUrl", String.class);
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey())
            .build()
            .parseSignedClaims(token);
    }

    public LocalDateTime getAccessTokenExpiresAt() {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwt().getAccessExpSeconds());
        return LocalDateTime.ofInstant(exp, ZoneId.systemDefault());
    }
}
