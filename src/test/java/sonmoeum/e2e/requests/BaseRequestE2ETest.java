package sonmoeum.e2e.requests;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.users.entity.User;
import sonmoeum.e2e.AbstractE2ETest;
import sonmoeum.domain.classroom.enums.ClassroomType;

public abstract class BaseRequestE2ETest extends AbstractE2ETest {

    protected Long setupLessonForRequest() {
        // Create classroom
        Classroom classroom = new Classroom("1반",
            ClassroomType.WEEKDAY,
            "테스트용 분반");
        classroomRepository.save(classroom);

        // Create teacher
        User teacher = createTestUser("teacher@example.com", "password");

        // Create subject
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

        // Create lesson manually for testing
        Lesson lesson = new Lesson(
                subject,
                teacher,
                LocalDate.of(2026, 2, 3), // A Monday
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                Lesson.AttendanceStatus.PENDING);
        lessonRepository.save(lesson);

        return lesson.getId();
    }

    protected Map<String, Object> createAbsenceRequestBody(Long lessonId, String reason) {
        return Map.of(
            "lessonId", lessonId,
            "reason", reason
        );
    }

    protected Map<String, Object> createPurchaseRequestBody(String itemName, int quantity, int estimatedPrice) {
        return Map.of(
            "itemName", itemName,
            "quantity", quantity,
            "estimatedPrice", estimatedPrice
        );
    }

    protected String getAdminSession() {
        return loginAsAdmin();
    }

    protected String getVolunteerSession() {
        return loginAsVolunteer();
    }
}
