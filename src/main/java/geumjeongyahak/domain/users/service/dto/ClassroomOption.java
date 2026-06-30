package geumjeongyahak.domain.users.service.dto;

import geumjeongyahak.domain.classroom.entity.Classroom;

public record ClassroomOption(
    Long id,
    String name
) {
    public static ClassroomOption from(Classroom classroom) {
        return new ClassroomOption(classroom.getId(), classroom.getName());
    }
}
