package sonmoeum.domain.student.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import sonmoeum.domain.student.entity.Student;

public record StudentResponse(
    @Schema(description = "학생 식별자", example = "1")
    Long id,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "설명", example = "학생입니다.")
    String description,

    @Schema(description = "상태", example = "ENROLLED")
    String status
) {

    public static StudentResponse from(Student savedStudent) {
        return new StudentResponse(
            savedStudent.getId(),
            savedStudent.getName(),
            savedStudent.getPhoneNumber(),
            savedStudent.getDescription(),
            savedStudent.getStatus().name()
        );
    }
}
