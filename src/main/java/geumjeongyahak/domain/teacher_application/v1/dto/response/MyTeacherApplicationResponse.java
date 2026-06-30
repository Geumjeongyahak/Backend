package geumjeongyahak.domain.teacher_application.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 교원 신청 상태 응답")
public record MyTeacherApplicationResponse(

    @Schema(description = "조회 가능한 교원 신청 존재 여부", example = "true")
    boolean exists,

    @Schema(description = "내 최신 교원 신청. 없으면 null입니다.", nullable = true)
    TeacherApplicationResponse application
) {
    public static MyTeacherApplicationResponse exists(TeacherApplicationResponse application) {
        return new MyTeacherApplicationResponse(true, application);
    }

    public static MyTeacherApplicationResponse empty() {
        return new MyTeacherApplicationResponse(false, null);
    }
}
