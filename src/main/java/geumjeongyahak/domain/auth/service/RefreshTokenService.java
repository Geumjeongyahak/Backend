package geumjeongyahak.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import geumjeongyahak.common.security.config.SecurityProperties;
import geumjeongyahak.domain.auth.entity.RefreshToken;
import geumjeongyahak.domain.auth.repository.RefreshTokenRepository;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    // ponytail: 계정당 기기 5대 상한, 넘으면 가장 오래된 기기가 다음 재발급 때 다시 로그인. 기기 관리 화면이 생기면 설정값으로
    private static final int MAX_ACTIVE_REFRESH_TOKENS_PER_CREDENTIAL = 5;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final SecurityProperties securityProperties;

    /**
     * Refresh Token 생성 및 DB 저장
     * @param credentialId 자격 증명 ID
     * @return 생성된 Refresh Token 문자열
     */
    @Transactional
    public String createRefreshToken(Long credentialId) {
        log.debug("Refresh Token 생성 요청: credentialId={}", credentialId);

        pruneTokens(credentialId);

        // 새로운 Refresh Token 생성
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwt().getRefreshExpSeconds());
        LocalDateTime expiryDate = LocalDateTime.ofInstant(exp, ZoneId.systemDefault());

        // UUID 기반 고유 토큰 생성
        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .credentialId(credentialId)
                .expiryDate(expiryDate)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.debug("Refresh Token 생성 완료: credentialId={}, expiresAt={}", credentialId, expiryDate);
        return tokenValue;
    }

    /**
     * 새 토큰 자리를 만든다: 만료된 토큰과, 새 토큰을 더하면 상한을 넘는 가장 오래된 토큰을 지운다.
     */
    private void pruneTokens(Long credentialId) {
        List<RefreshToken> live = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (RefreshToken token : refreshTokenRepository.findByCredentialId(credentialId)) {
            if (token.isExpired()) {
                stale.add(token.getToken());
            } else {
                live.add(token);
            }
        }
        live.sort(Comparator.comparing(RefreshToken::getCreatedAt).reversed());
        live.stream()
                .skip(MAX_ACTIVE_REFRESH_TOKENS_PER_CREDENTIAL - 1)
                .forEach(token -> stale.add(token.getToken()));
        if (!stale.isEmpty()) {
            refreshTokenRepository.deleteAllByIdInBatch(stale);
        }
    }

    /**
     * Refresh Token 검증
     * @param token Refresh Token 문자열
     * @return 유효하면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean validateRefreshToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.debug("Refresh Token 검증 실패: 토큰이 비어있음");
            return false;
        }

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findById(token);
        boolean isValid = refreshToken.isPresent() && !refreshToken.get().isExpired();

        log.debug("Refresh Token 검증 결과: {}", isValid);
        return isValid;
    }

    /**
     * Refresh Token으로 credential ID 조회
     * @param token Refresh Token 문자열
     * @return credential ID (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<Long> getCredentialIdFromRefreshToken(String token) {
        log.debug("Refresh Token에서 credential ID 조회 요청");

        return refreshTokenRepository.findById(token)
                .filter(rt -> !rt.isExpired())
                .map(RefreshToken::getCredentialId);
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     * @param token Refresh Token 문자열
     */
    @Transactional
    public void deleteRefreshToken(String token) {
        log.debug("Refresh Token 삭제 요청");

        if (StringUtils.hasText(token)) {
            // 없는 행이어도 예외 없이 지나간다 (동시 재발급·중복 로그아웃)
            refreshTokenRepository.deleteAllByIdInBatch(List.of(token));
            log.info("Refresh Token 삭제 완료");
        }
    }

    /**
     * 사용자 ID로 Refresh Token 삭제 (전체 디바이스 로그아웃)
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteRefreshTokenByUserId(Long userId) {
        log.debug("사용자 ID로 Refresh Token 삭제: userId={}", userId);
        List<Long> credentialIds = userCredentialRepository.findAllByUserId(userId).stream()
            .map(credential -> credential.getId())
            .toList();
        if (!credentialIds.isEmpty()) {
            refreshTokenRepository.deleteByCredentialIdIn(credentialIds);
        }
        log.info("사용자의 모든 Refresh Token 삭제 완료: userId={}", userId);
    }

    /**
     * 만료된 Refresh Token 일괄 삭제 (스케줄러에서 주기적으로 실행)
     */
    @Transactional
    public void deleteExpiredRefreshTokens() {
        log.debug("만료된 Refresh Token 일괄 삭제 시작");

        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("만료된 Refresh Token 일괄 삭제 완료");
    }

    /**
     * Refresh Token이 존재하는지 확인
     * @param credentialId 자격 증명 ID
     * @return 존재하면 true
     */
    @Transactional(readOnly = true)
    public boolean existsByCredentialId(Long credentialId) {
        return refreshTokenRepository.existsByCredentialId(credentialId);
    }

    /**
     * Refresh Token 만료 시각 계산
     * @return Refresh Token 만료 시각
     */
    public LocalDateTime getRefreshTokenExpiresAt() {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwt().getRefreshExpSeconds());
        return LocalDateTime.ofInstant(exp, ZoneId.systemDefault());
    }
}
