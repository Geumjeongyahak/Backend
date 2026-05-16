package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleNotFoundException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleStatusRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyScheduleAdminService {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailyScheduleService dailyScheduleService;
    private final LessonProxyService lessonProxyService;

    @Transactional
    public DailyScheduleDetailResponse updateStatus(
        Long dailyScheduleId,
        Long adminId,
        boolean canViewSensitiveInfo,
        UpdateDailyScheduleStatusRequest request
    ) {
        log.debug(
            "DailySchedule 관리자 상태 변경 요청 (dailyScheduleId={}, adminId={}, status={})",
            dailyScheduleId,
            adminId,
            request.status()
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 관리자 상태 변경 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        dailySchedule.updateStatus(request.status());
        lessonProxyService.updateActiveLessonsStatusByClassroomAndDate(
            dailySchedule.getClassroom().getId(),
            dailySchedule.getLessonDate(),
            toLessonStatus(request.status())
        );

        log.debug("DailySchedule 관리자 상태 변경 완료 (dailyScheduleId={}, status={})", dailyScheduleId, request.status());
        return dailyScheduleService.getDailySchedule(dailyScheduleId, adminId, canViewSensitiveInfo);
    }

    private LessonStatus toLessonStatus(DailyScheduleStatus status) {
        return switch (status) {
            case SCHEDULED -> LessonStatus.SCHEDULED;
            case COMPLETED -> LessonStatus.COMPLETED;
            case CANCELLED -> LessonStatus.CANCELED;
        };
    }
}
