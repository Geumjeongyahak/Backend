package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.exception.DailyTeacherAttendanceRequiredException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleNotFoundException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleAttendanceStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyTeacherCheckOutTimeException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleStatusRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceCorrectionRequest;
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
    private final DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;
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

    @Transactional
    public DailyScheduleDetailResponse correctTeacherAttendance(
        Long dailyScheduleId,
        Long adminId,
        boolean canViewSensitiveInfo,
        UpdateDailyTeacherAttendanceCorrectionRequest request
    ) {
        log.debug(
            "DailySchedule 관리자 교사 출석 보정 요청 (dailyScheduleId={}, adminId={}, status={}, attendedAt={}, checkedOutAt={})",
            dailyScheduleId,
            adminId,
            request.status(),
            request.attendedAt(),
            request.checkedOutAt()
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 관리자 교사 출석 보정 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        validateTeacherAttendanceCorrectionState(dailySchedule);

        DailyTeacherAttendance teacherAttendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info(
                    "DailySchedule 관리자 교사 출석 보정 실패 - 교사 출석 기록이 필요합니다. dailyScheduleId={}",
                    dailyScheduleId
                );
                return new DailyTeacherAttendanceRequiredException(dailyScheduleId);
            });
        validateTeacherAttendanceCorrection(dailySchedule, request);

        teacherAttendance.correctAttendance(request.status(), request.attendedAt(), request.checkedOutAt());
        dailyScheduleService.recalculateCompletionStatus(dailySchedule);

        log.debug(
            "DailySchedule 관리자 교사 출석 보정 완료 (dailyScheduleId={}, status={}, attendedAt={}, checkedOutAt={})",
            dailyScheduleId,
            request.status(),
            request.attendedAt(),
            request.checkedOutAt()
        );
        return dailyScheduleService.getDailySchedule(dailyScheduleId, adminId, canViewSensitiveInfo);
    }

    private LessonStatus toLessonStatus(DailyScheduleStatus status) {
        return switch (status) {
            case SCHEDULED -> LessonStatus.SCHEDULED;
            case COMPLETED -> LessonStatus.COMPLETED;
            case CANCELLED -> LessonStatus.CANCELED;
        };
    }

    private void validateTeacherAttendanceCorrectionState(DailySchedule dailySchedule) {
        if (dailySchedule.getStatus() != DailyScheduleStatus.CANCELLED) {
            return;
        }

        log.info(
            "DailySchedule 관리자 교사 출석 보정 실패 - 휴강 상태에서는 보정할 수 없습니다. dailyScheduleId={}, status={}",
            dailySchedule.getId(),
            dailySchedule.getStatus()
        );
        throw new InvalidDailyScheduleAttendanceStateException(dailySchedule.getId(), dailySchedule.getStatus());
    }

    private void validateTeacherAttendanceCorrection(
        DailySchedule dailySchedule,
        UpdateDailyTeacherAttendanceCorrectionRequest request
    ) {
        if (request.attendedAt() != null
            && request.checkedOutAt() != null
            && request.checkedOutAt().isBefore(request.attendedAt())) {
            log.info(
                "DailySchedule 관리자 교사 출석 보정 실패 - 퇴근 시간이 출근 시간보다 빠릅니다. dailyScheduleId={}, attendedAt={}, checkedOutAt={}",
                dailySchedule.getId(),
                request.attendedAt(),
                request.checkedOutAt()
            );
            throw new InvalidDailyTeacherCheckOutTimeException(
                dailySchedule.getId(),
                request.attendedAt(),
                request.checkedOutAt()
            );
        }
    }
}
