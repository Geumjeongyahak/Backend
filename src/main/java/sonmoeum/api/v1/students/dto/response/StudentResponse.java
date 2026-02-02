package sonmoeum.api.v1.students.dto.response;

import sonmoeum.domain.student.entity.Student;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudentResponse(
    @Schema(description = "학생 ID", example = "1")
    Long id,
    @Schema(description = "학생 이름", example = "김학생")
    String name,
    @Schema(description = "학생 전화번호", example = "010-1111-2222")
    String phoneNumber,
    @Schema(description = "학생 설명", example = "특이사항 없음")
    String description
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getName(),
            student.getPhoneNumber(),
            student.getDescription()
        );
    }
}
