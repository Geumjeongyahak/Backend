package geumjeongyahak.unit.auth;

import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.auth.service.RefreshTokenService;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.auth.service.event.UserDeactivatedEventHandler;
import geumjeongyahak.domain.users.event.UserDeactivatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeactivatedEventHandlerTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserCredentialService userCredentialService;

    @InjectMocks
    private UserDeactivatedEventHandler eventHandler;

    @Test
    void handleUserDeactivated_deletesRefreshTokensAndClearsPasswordResetTokens() {
        Long userId = 10L;

        eventHandler.handleUserDeactivated(new UserDeactivatedEvent(userId));

        verify(refreshTokenService).deleteRefreshTokenByUserId(userId);
        verify(userCredentialService).clearPasswordResetTokensByUserId(userId);
    }
}
