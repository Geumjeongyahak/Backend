package geumjeongyahak.domain.student.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;
import java.util.List;

@Schema(description = "학생 생성 요청 DTO")
public record CreateStudentRequest(
    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "전화번호", example = "010-1234-5678")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "설명", example = "학생입니다.")
    String description,

    @Schema(description = "소속 분반 식별자 목록", example = "[1, 2]")
    @NotEmpty(message = "분반 ID 목록은 필수입니다.")
    List<@NotNull(message = "분반 ID는 필수입니다.") Long> classroomIds
) {}
