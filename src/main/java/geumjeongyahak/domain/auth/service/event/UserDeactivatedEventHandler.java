package geumjeongyahak.domain.auth.service.event;

import geumjeongyahak.domain.auth.service.RefreshTokenService;
import geumjeongyahak.domain.users.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeactivatedEventHandler {

    private final RefreshTokenService refreshTokenService;

    @EventListener
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        refreshTokenService.deleteRefreshTokenByUserId(event.userId());
        log.info("비활성 사용자 Refresh Token 폐기 완료 - userId: {}", event.userId());
    }
}
