package geumjeongyahak.domain.users.v1.dto.response;

import geumjeongyahak.domain.users.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교사 연락망 화면에서 사용하는 현재 활동 교사 정보입니다.")
public record TeacherContactResponse(
    @Schema(description = "교사 사용자 ID", example = "2")
    Long id,

    @Schema(description = "교사 이름", example = "홍길동")
    String name,

    @Schema(description = "교사가 소속된 반 이름입니다. 소속 반이 없으면 null입니다.", example = "국화반", nullable = true)
    String classroomName,

    @Schema(description = "교사 연락처", example = "010-1234-5678", nullable = true)
    String phoneNumber
) {
    public static TeacherContactResponse from(User user) {
        return new TeacherContactResponse(
            user.getId(),
            user.getName(),
            user.getClassroom() != null ? user.getClassroom().getName() : null,
            user.getPhoneNumber()
        );
    }
}
