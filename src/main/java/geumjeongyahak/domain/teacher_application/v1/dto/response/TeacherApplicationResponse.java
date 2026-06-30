package geumjeongyahak.domain.teacher_application.v1.dto.response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.teacher_application.entity.TeacherApplication;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교원 신청 응답")
public record TeacherApplicationResponse(

    @Schema(description = "교원 신청 ID", example = "1")
    Long id,

    @Schema(description = "신청자 User ID", example = "5")
    Long applicantId,

    @Schema(description = "신청 당시 이름", example = "홍길동")
    String applicantName,

    @Schema(description = "신청 당시 연락처", example = "010-0000-0000")
    String applicantPhoneNumber,

    @Schema(description = "신청 당시 이메일", example = "hong@example.com")
    String applicantEmail,

    @Schema(description = "생년월일", example = "1999-03-15")
    LocalDate birthDate,

    @Schema(description = "주소", example = "부산광역시 금정구")
    String address,

    @Schema(description = "최종 학력 및 전공", example = "부산대학교 국어국문학과 졸업")
    String educationAndMajor,

    @Schema(description = "선호 과목 ID", example = "3")
    Long preferredSubjectId,

    @Schema(description = "선호 과목명", example = "국어")
    String preferredSubjectName,

    @Schema(description = "선호 과목의 분반명", example = "국화반")
    String preferredClassroomName,

    @Schema(description = "선호 과목 요일", example = "FRIDAY")
    DayOfWeek preferredDayOfWeek,

    @Schema(description = "선호 과목 시작 시간", example = "19:00:00")
    LocalTime preferredStartTime,

    @Schema(description = "선호 과목 종료 시간", example = "22:00:00")
    LocalTime preferredEndTime,

    @Schema(description = "실제 배정 시간표 과목 목록. 승인 전이면 빈 배열입니다.")
    List<AssignedSubjectResponse> assignedSubjects,

    @Schema(description = "실제 배정 과목의 분반 ID. 승인 전이면 null입니다.", example = "1", nullable = true)
    Long assignedClassroomId,

    @Schema(description = "실제 배정 과목의 분반명. 승인 전이면 null입니다.", example = "국화반", nullable = true)
    String assignedClassroomName,

    @Schema(description = "실제 배정 과목 요일. 승인 전이면 null입니다.", example = "FRIDAY", nullable = true)
    DayOfWeek assignedDayOfWeek,

    @Schema(description = "실제 배정 과목 시작 시간. 승인 전이면 null입니다.", example = "19:00:00", nullable = true)
    LocalTime assignedStartTime,

    @Schema(description = "실제 배정 과목 종료 시간. 승인 전이면 null입니다.", example = "22:00:00", nullable = true)
    LocalTime assignedEndTime,

    @Schema(description = "지원 동기")
    String motivation,

    @Schema(description = "희망하는 교사상")
    String desiredTeacherImage,

    @Schema(description = "나눔의 의미")
    String meaningOfSharing,

    @Schema(description = "교원 신청 상태", example = "PENDING")
    TeacherApplicationStatus status,

    @Schema(description = "승인/반려 시각")
    LocalDateTime reviewedAt,

    @Schema(description = "처리자 이름")
    String reviewedByName,

    @Schema(description = "승인 메모 또는 반려 사유")
    String reviewNote,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt,

    @Schema(description = "수정 시각")
    LocalDateTime updatedAt
) {
    public static TeacherApplicationResponse from(TeacherApplication application) {
        Subject preferredSubject = application.getPreferredSubject();
        List<AssignedSubjectResponse> assignedSubjects = application.getAssignedSubjects()
            .stream()
            .sorted(Comparator.comparing(Subject::getPeriod)
                .thenComparing(Subject::getStartTime)
                .thenComparing(Subject::getId))
            .map(AssignedSubjectResponse::from)
            .toList();
        Subject representativeAssignedSubject = application.getAssignedSubjects()
            .stream()
            .findFirst()
            .orElse(null);
        LocalTime assignedStartTime = application.getAssignedSubjects()
            .stream()
            .map(Subject::getStartTime)
            .min(LocalTime::compareTo)
            .orElse(null);
        LocalTime assignedEndTime = application.getAssignedSubjects()
            .stream()
            .map(Subject::getEndTime)
            .max(LocalTime::compareTo)
            .orElse(null);
        return new TeacherApplicationResponse(
            application.getId(),
            application.getApplicant().getId(),
            application.getApplicantName(),
            application.getApplicantPhoneNumber(),
            application.getApplicantEmail(),
            application.getBirthDate(),
            application.getAddress(),
            application.getEducationAndMajor(),
            preferredSubject.getId(),
            preferredSubject.getName(),
            preferredSubject.getClassroom().getName(),
            preferredSubject.getDayOfWeek(),
            preferredSubject.getStartTime(),
            preferredSubject.getEndTime(),
            assignedSubjects,
            representativeAssignedSubject != null ? representativeAssignedSubject.getClassroom().getId() : null,
            representativeAssignedSubject != null ? representativeAssignedSubject.getClassroom().getName() : null,
            representativeAssignedSubject != null ? representativeAssignedSubject.getDayOfWeek() : null,
            assignedStartTime,
            assignedEndTime,
            application.getMotivation(),
            application.getDesiredTeacherImage(),
            application.getMeaningOfSharing(),
            application.getStatus(),
            application.getReviewedAt(),
            application.getReviewedBy() != null ? application.getReviewedBy().getName() : null,
            application.getReviewNote(),
            application.getCreatedAt(),
            application.getUpdatedAt()
        );
    }
}
