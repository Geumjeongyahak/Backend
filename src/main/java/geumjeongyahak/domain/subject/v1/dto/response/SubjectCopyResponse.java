package geumjeongyahak.domain.subject.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "시간표 기간 복사 결과")
public record SubjectCopyResponse(
    @Schema(description = "복사한 과목 수", example = "3")
    int copiedCount,

    @Schema(description = "새로 만든 과목")
    List<SubjectDetailResponse> subjects
) {

    public static SubjectCopyResponse of(List<SubjectDetailResponse> subjects) {
        return new SubjectCopyResponse(subjects.size(), subjects);
    }
}
