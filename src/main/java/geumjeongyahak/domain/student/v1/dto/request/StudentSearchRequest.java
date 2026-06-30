package geumjeongyahak.domain.student.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import geumjeongyahak.domain.student.enums.StudentStatus;

@Schema(description = "학생 목록 조회 요청 DTO")
@Getter
@Setter
public class StudentSearchRequest {

    @Schema(description = "이름 검색(부분 일치)", example = "홍")
    private String name;

    @Schema(description = "학생 상태", example = "ENROLLED")
    private StudentStatus status;

    @Schema(description = "분반 식별자", example = "1")
    private Long classroomId;
}
