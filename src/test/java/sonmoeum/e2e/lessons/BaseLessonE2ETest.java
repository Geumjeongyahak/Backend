package sonmoeum.e2e.lessons;

import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.users.entity.User;
import sonmoeum.e2e.AbstractE2ETest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import sonmoeum.domain.classroom.enums.ClassroomType;

public abstract class BaseLessonE2ETest extends AbstractE2ETest {

    protected Long setupSubjectAndLesson() {
        // Create classroom
        Classroom classroom = new Classroom("1반",
            ClassroomType.WEEKDAY,
            "테스트용 분반");
        classroomRepository.save(classroom);

        // Create teacher
        User teacher = createTestUser("teacher@example.com", "password");

        // Create subject (lessons will be auto-created via event)
        Subject subject = new Subject(
                classroom,
                teacher,
                "수학",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 6, 30),
                20, // times
                DayOfWeek.MONDAY,
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                1, // period
                "테스트 과목");
        subjectRepository.save(subject);

        // Return first lesson ID (assuming at least one lesson is created)
        return lessonRepository.findAll().get(0).getId();
    }

    protected String getAdminSession() {
        return loginAsAdmin();
    }
}
