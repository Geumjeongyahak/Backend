package geumjeongyahak.domain.subject.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "과목 담당 교사 배정 요청")
public record AssignSubjectTeacherRequest(

    @Schema(description = "담당 교사 ID. null이면 과목 담당 교사를 비웁니다. 봉사자, 매니저 또는 관리자 사용자만 배정할 수 있습니다.", example = "2", nullable = true)
    Long teacherId
) {
}
