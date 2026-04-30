package geumjeongyahak.domain.channel.aspect;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.channel.annotation.RequireChannelAccess;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.exception.ChannelErrorCode;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.domain.channel.service.ChannelAccessLevelChecker;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ChannelAccessAspect {

    private final ChannelRepository channelRepository;
    private final ChannelAccessLevelChecker accessLevelChecker;

    @Around("@annotation(requireChannelAccess)")
    public Object checkAccess(ProceedingJoinPoint pjp, RequireChannelAccess requireChannelAccess) throws Throwable {
        Long channelId = extractChannelId(pjp, requireChannelAccess.channelIdParam());
        CustomUserDetails userDetails = getCurrentUser();

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isDeleted() || !channel.isActive()) {
            throw new ResourceNotFoundException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }

        if (!accessLevelChecker.canAccess(channel, requireChannelAccess.minLevel(), userDetails)) {
            throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
        }

        return pjp.proceed();
    }

    private Long extractChannelId(ProceedingJoinPoint pjp, String paramName) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i]) && args[i] instanceof Long id) {
                return id;
            }
        }
        throw new IllegalStateException("@RequireChannelAccess: '" + paramName + "' 파라미터를 찾을 수 없습니다.");
    }

    private CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
        return (CustomUserDetails) auth.getPrincipal();
    }
}
