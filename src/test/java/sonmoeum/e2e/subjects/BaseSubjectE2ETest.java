package sonmoeum.e2e.subjects;

import java.util.Map;

import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.users.entity.User;
import sonmoeum.e2e.AbstractE2ETest;
import sonmoeum.domain.classroom.enums.ClassroomType;

public abstract class BaseSubjectE2ETest extends AbstractE2ETest {

    protected Long createClassroom() {
        Classroom classroom = new Classroom("1반",
            ClassroomType.WEEKDAY,
            "테스트용 분반");
        return classroomRepository.save(classroom).getId();
    }

    protected Long createTeacher() {
        User teacher = createTestUser("teacher@example.com", "password");
        return teacher.getId();
    }

    protected Map<String, Object> createSubjectRequest(Long classroomId, Long teacherId) {
        return Map.of(
            "classId", classroomId,
            "teacherId", teacherId,
            "name", "수학",
            "dayOfWeek", "MONDAY",
            "startTime", "14:00:00",
            "endTime", "15:00:00",
            "startAt", "2026-02-01",
            "endAt", "2026-06-30",
            "times", 20,
            "period", 60
        );
    }

    protected String getAdminSession() {
        return loginAsAdmin();
    }
}
