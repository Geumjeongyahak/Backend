package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleJournalSheetLinkNotConfiguredException;
import geumjeongyahak.domain.daily_schedule.exception.DailyTeacherAttendanceRequiredException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleNotFoundException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleAttendanceStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyTeacherCheckOutTimeException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleStatusRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceCorrectionRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleJournalSheetLinkResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleJournalSheetRowResponse;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyScheduleAdminService {

    @Value("${DAILY_SCHEDULE_JOURNAL_SHEET_URL:}")
    private String journalSheetUrl;

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;
    private final DailyScheduleService dailyScheduleService;
    private final LessonProxyService lessonProxyService;

    public DailyScheduleJournalSheetLinkResponse getJournalSheetLink() {
        if (!StringUtils.hasText(journalSheetUrl)) {
            throw new DailyScheduleJournalSheetLinkNotConfiguredException();
        }
        return new DailyScheduleJournalSheetLinkResponse(journalSheetUrl.trim());
    }

    public List<DailyScheduleJournalSheetRowResponse> getMonthlyJournalSheetData(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        log.debug("DailySchedule 월별 수업일지 시트 조회 요청 (month={}, from={}, to={})", month, from, to);

        List<DailySchedule> dailySchedules = dailyScheduleRepository
            .findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(from, to);
        if (dailySchedules.isEmpty()) {
            return List.of();
        }

        Map<DailyScheduleLessonKey, List<Lesson>> lessonsByScheduleKey = getLessonsByScheduleKey(dailySchedules);
        List<DailySchedule> writtenSchedules = dailySchedules.stream()
            .filter(dailySchedule -> hasWrittenJournal(
                lessonsByScheduleKey.getOrDefault(DailyScheduleLessonKey.from(dailySchedule), List.of())
            ))
            .toList();
        if (writtenSchedules.isEmpty()) {
            return List.of();
        }

        List<Long> dailyScheduleIds = writtenSchedules.stream()
            .map(DailySchedule::getId)
            .toList();
        Map<Long, DailyTeacherAttendance> attendanceByScheduleId = dailyTeacherAttendanceRepository
            .findAllByDailyScheduleIdInAndIsDeletedFalse(dailyScheduleIds)
            .stream()
            .collect(Collectors.toMap(
                attendance -> attendance.getDailySchedule().getId(),
                Function.identity()
            ));

        List<DailyScheduleJournalSheetRowResponse> responses = writtenSchedules.stream()
            .map(dailySchedule -> DailyScheduleJournalSheetRowResponse.of(
                dailySchedule,
                attendanceByScheduleId.get(dailySchedule.getId()),
                lessonsByScheduleKey.getOrDefault(DailyScheduleLessonKey.from(dailySchedule), List.of())
            ))
            .toList();
        log.debug("DailySchedule 월별 수업일지 시트 조회 완료 (month={}, count={})", month, responses.size());
        return responses;
    }

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

    private Map<DailyScheduleLessonKey, List<Lesson>> getLessonsByScheduleKey(
        List<DailySchedule> dailySchedules
    ) {
        Set<Long> classroomIds = dailySchedules.stream()
            .map(dailySchedule -> dailySchedule.getClassroom().getId())
            .collect(Collectors.toSet());
        Set<LocalDate> lessonDates = dailySchedules.stream()
            .map(DailySchedule::getLessonDate)
            .collect(Collectors.toSet());

        return lessonProxyService.getActiveLessonsByClassroomIdsAndDates(classroomIds, lessonDates)
            .stream()
            .collect(Collectors.groupingBy(DailyScheduleLessonKey::from));
    }

    private boolean hasWrittenJournal(List<Lesson> lessons) {
        return lessons.stream().anyMatch(lesson -> lesson.getNote() != null && !lesson.getNote().isBlank());
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

    private record DailyScheduleLessonKey(Long classroomId, LocalDate lessonDate) {

        private static DailyScheduleLessonKey from(DailySchedule dailySchedule) {
            return new DailyScheduleLessonKey(
                dailySchedule.getClassroom().getId(),
                dailySchedule.getLessonDate()
            );
        }

        private static DailyScheduleLessonKey from(Lesson lesson) {
            return new DailyScheduleLessonKey(
                lesson.getSubject().getClassroom().getId(),
                lesson.getDate()
            );
        }
    }
}
