package geumjeongyahak.domain.teacher_application.v1.dto.response;

import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.teacher_application.service.dto.ScheduleGroupKey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Schema(description = "교원 신청 가능 시간표 응답")
public record AvailableTeacherScheduleResponse(

    @Schema(description = "시간표 후보 키", example = "1:FRIDAY:2026-03-01:2026-06-30")
    String scheduleKey,

    @Schema(description = "분반 ID", example = "1")
    Long classroomId,

    @Schema(description = "분반명", example = "벚꽃반")
    String classroomName,

    @Schema(description = "요일", example = "FRIDAY")
    DayOfWeek dayOfWeek,

    @Schema(description = "운영 시작일", example = "2026-03-01")
    LocalDate startAt,

    @Schema(description = "운영 종료일", example = "2026-06-30")
    LocalDate endAt,

    @Schema(description = "하루 시간표 시작 시간", example = "19:00:00")
    LocalTime startTime,

    @Schema(description = "하루 시간표 종료 시간", example = "22:00:00")
    LocalTime endTime,

    @Schema(description = "승인 요청에 사용할 시간표 과목 ID 목록", example = "[100, 101]")
    List<Long> subjectIds,

    @Schema(description = "시간표 과목 목록")
    List<AvailableTeacherScheduleSubjectResponse> subjects
) {
    public static AvailableTeacherScheduleResponse from(ScheduleGroupKey key, List<Subject> subjects) {
        List<Subject> sortedSubjects = subjects.stream()
            .sorted(Comparator.comparing(Subject::getPeriod)
                .thenComparing(Subject::getStartTime)
                .thenComparing(Subject::getId))
            .toList();
        return new AvailableTeacherScheduleResponse(
            key.value(),
            key.classroomId(),
            sortedSubjects.get(0).getClassroom().getName(),
            key.dayOfWeek(),
            key.startAt(),
            key.endAt(),
            sortedSubjects.stream().map(Subject::getStartTime).min(LocalTime::compareTo).orElse(null),
            sortedSubjects.stream().map(Subject::getEndTime).max(LocalTime::compareTo).orElse(null),
            sortedSubjects.stream().map(Subject::getId).toList(),
            sortedSubjects.stream().map(AvailableTeacherScheduleSubjectResponse::from).toList()
        );
    }
}
