package geumjeongyahak.domain.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonExchangeRequestExpirationScheduler {

    private final LessonExchangeRequestService lessonExchangeRequestService;

    @Scheduled(cron = "0 * * * * *")
    public void expireLessonExchangeRequests() {
        int expiredCount = lessonExchangeRequestService.expireExpiredLessonExchangeRequests();

        if (expiredCount > 0) {
            log.info("수업 교환 요청 자동 만료 스케줄러 실행 완료 (count={})", expiredCount);
        }
    }
}
