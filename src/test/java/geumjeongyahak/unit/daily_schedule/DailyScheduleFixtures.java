package geumjeongyahak.unit.daily_schedule;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyStudentAttendanceStatus;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.test.util.ReflectionTestUtils;

/** daily_schedule 단위 테스트가 함께 쓰는 엔티티 생성기. */
final class DailyScheduleFixtures {

    private DailyScheduleFixtures() {
    }

    static Classroom classroom(Long id) {
        return classroom(id, "장미반");
    }

    static Classroom classroom(Long id, String name) {
        Classroom classroom = Classroom.builder()
            .name(name)
            .type(ClassroomType.WEEKDAY)
            .build();
        ReflectionTestUtils.setField(classroom, "id", id);
        return classroom;
    }

    static User teacher(String name) {
        return User.builder()
            .name(name)
            .role(RoleType.VOLUNTEER)
            .build();
    }

    static User teacher(Long id, String name) {
        User teacher = teacher(name);
        ReflectionTestUtils.setField(teacher, "id", id);
        return teacher;
    }

    static DailySchedule dailySchedule(Long id, Classroom classroom, User teacher, LocalDate lessonDate) {
        DailySchedule dailySchedule = new DailySchedule(
            classroom,
            teacher,
            lessonDate,
            LocalTime.of(19, 20),
            LocalTime.of(21, 40)
        );
        ReflectionTestUtils.setField(dailySchedule, "id", id);
        return dailySchedule;
    }

    static Subject subject(Classroom classroom, User teacher, LocalDate lessonDate) {
        return new Subject(
            classroom,
            teacher,
            "국어",
            lessonDate,
            lessonDate.plusMonths(1),
            DayOfWeek.WEDNESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(16, 0),
            1,
            LocalDateTime.now(),
            null
        );
    }

    static Lesson lesson(
        Subject subject,
        User teacher,
        LocalDate lessonDate,
        LocalTime startTime,
        LocalTime endTime,
        int period
    ) {
        return new Lesson(subject, teacher, lessonDate, startTime, endTime, period);
    }

    static Student student(Long id, Classroom classroom) {
        return student(id, "최양지", classroom);
    }

    static Student student(Long id, String name, Classroom classroom) {
        Student student = new Student(name, null, null, classroom);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    static DailyStudentAttendance studentAttendance(
        Long id,
        DailySchedule dailySchedule,
        Student student,
        DailyStudentAttendanceStatus status
    ) {
        DailyStudentAttendance attendance = new DailyStudentAttendance(dailySchedule, student);
        ReflectionTestUtils.setField(attendance, "id", id);
        attendance.updateStatus(status);
        return attendance;
    }
}
