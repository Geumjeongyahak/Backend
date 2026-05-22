package geumjeongyahak.domain.student.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.student.entity.Student;
import java.util.List;
import java.util.stream.Collectors;

public record StudentResponse(
    @Schema(description = "학생 식별자", example = "1")
    Long id,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "설명", example = "학생입니다.")
    String description,

    @Schema(description = "소속 분반 목록")
    List<ClassroomSummary> classrooms,

    @Schema(description = "상태", example = "ENROLLED")
    String status
) {

    public static StudentResponse from(Student savedStudent) {
        return new StudentResponse(
            savedStudent.getId(),
            savedStudent.getName(),
            savedStudent.getPhoneNumber(),
            savedStudent.getDescription(),
            savedStudent.getClassrooms().stream()
                .map(ClassroomSummary::from)
                .toList(),
            savedStudent.getStatus().name()
        );
    }

    public String classroomNames() {
        return classrooms.stream()
            .map(ClassroomSummary::name)
            .collect(Collectors.joining(", "));
    }

    public List<Long> classroomIds() {
        return classrooms.stream()
            .map(ClassroomSummary::id)
            .toList();
    }

    public record ClassroomSummary(
        @Schema(description = "분반 식별자", example = "1")
        Long id,

        @Schema(description = "분반 이름", example = "벚꽃반")
        String name
    ) {
        private static ClassroomSummary from(Classroom classroom) {
            return new ClassroomSummary(classroom.getId(), classroom.getName());
        }
    }
}
