package geumjeongyahak.unit.base;

import static geumjeongyahak.unit.base.InitialDataSqlReader.longValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("개발 초기 데이터 커버리지 테스트")
class InitialDataDevSeedCoverageTest {

    private static final Path INITIAL_DATA = Path.of("src/main/resources/sql/init_data.sql");
    private static final LocalDate RICH_SEED_FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate RICH_SEED_TO = LocalDate.of(2026, 9, 30);
    private static final Set<String> NON_CLASSROOM_TEACHER_EMAILS = Set.of(
        "geumjeongyahak-apps-script-bot@gmail.com"
    );

    @Test
    @DisplayName("개발 seed 교사, 학생, 일정, 썸네일은 로컬 화면 테스트에 충분해야 한다")
    void developmentSeed_isRichEnoughForLocalTesting() throws IOException {
        List<String> errors = new ArrayList<>();
        List<Map<String, String>> users = readTable("users");
        List<Map<String, String>> students = readTable("students");
        List<Map<String, String>> studentClassrooms = readTable("student_classrooms");
        List<Map<String, String>> schedules = readTable("daily_schedules");
        List<Map<String, String>> teacherAttendances = readTable("daily_teacher_attendances");
        List<Map<String, String>> studentAttendances = readTable("daily_student_attendances");
        List<Map<String, String>> posts = readTable("posts");

        requireTeacherClassroomMappings(errors, users);
        requireEnoughStudentsPerClassroom(errors, studentClassrooms);
        requireAbsoluteEventThumbnails(errors, posts);
        requireAttendancesForSeedSchedules(errors, studentClassrooms, schedules, teacherAttendances, studentAttendances);

        assertNoErrors(errors);
    }

    private static void requireTeacherClassroomMappings(List<String> errors, List<Map<String, String>> users) {
        users.stream()
            .filter(row -> "VOLUNTEER".equals(row.get("role")))
            .filter(row -> !NON_CLASSROOM_TEACHER_EMAILS.contains(row.get("primary_email")))
            .filter(row -> row.get("classroom_id") == null)
            .forEach(row -> errors.add("user " + row.get("id") + ": 교사 classroom_id가 없습니다."));
    }

    private static void requireEnoughStudentsPerClassroom(
        List<String> errors,
        List<Map<String, String>> studentClassrooms
    ) {
        Map<Long, Long> countsByClassroom = studentClassrooms.stream()
            .collect(Collectors.groupingBy(row -> longValue(row, "classroom_id"), Collectors.counting()));

        for (long classroomId = 1L; classroomId <= 9L; classroomId++) {
            long count = countsByClassroom.getOrDefault(classroomId, 0L);
            if (count < 5) {
                errors.add("classroom " + classroomId + ": 학생 수가 5명보다 적습니다. actual=" + count);
            }
        }
    }

    private static void requireAbsoluteEventThumbnails(List<String> errors, List<Map<String, String>> posts) {
        posts.stream()
            .filter(row -> "4".equals(row.get("channel_id")))
            .filter(row -> row.get("thumbnail_url") != null)
            .filter(row -> !row.get("thumbnail_url").startsWith("http://")
                && !row.get("thumbnail_url").startsWith("https://"))
            .forEach(row -> errors.add("post " + row.get("id") + ": 썸네일이 절대 URL이 아닙니다."));
    }

    private static void requireAttendancesForSeedSchedules(
        List<String> errors,
        List<Map<String, String>> studentClassrooms,
        List<Map<String, String>> schedules,
        List<Map<String, String>> teacherAttendances,
        List<Map<String, String>> studentAttendances
    ) {
        Map<Long, Long> studentCountsByClassroom = studentClassrooms.stream()
            .collect(Collectors.groupingBy(row -> longValue(row, "classroom_id"), Collectors.counting()));
        Map<Long, Long> teacherAttendanceCountsBySchedule = teacherAttendances.stream()
            .collect(Collectors.groupingBy(row -> longValue(row, "daily_schedule_id"), Collectors.counting()));
        Map<Long, Long> studentAttendanceCountsBySchedule = studentAttendances.stream()
            .collect(Collectors.groupingBy(row -> longValue(row, "daily_schedule_id"), Collectors.counting()));

        schedules.stream()
            .filter(InitialDataDevSeedCoverageTest::isRichSeedSchedule)
            .forEach(schedule -> {
                Long scheduleId = longValue(schedule, "id");
                Long classroomId = longValue(schedule, "classroom_id");
                long teacherAttendanceCount = teacherAttendanceCountsBySchedule.getOrDefault(scheduleId, 0L);
                long expectedStudentCount = studentCountsByClassroom.getOrDefault(classroomId, 0L);
                long studentAttendanceCount = studentAttendanceCountsBySchedule.getOrDefault(scheduleId, 0L);

                if (teacherAttendanceCount != 1L) {
                    errors.add("daily_schedule " + scheduleId + ": 교사 출석 row 수가 1이 아닙니다.");
                }
                if (studentAttendanceCount != expectedStudentCount) {
                    errors.add(
                        "daily_schedule " + scheduleId + ": 학생 출석 row 수가 반 학생 수와 다릅니다. expected="
                            + expectedStudentCount + ", actual=" + studentAttendanceCount
                    );
                }
            });
    }

    private static boolean isRichSeedSchedule(Map<String, String> schedule) {
        LocalDate lessonDate = LocalDate.parse(schedule.get("lesson_date"));
        return !lessonDate.isBefore(RICH_SEED_FROM) && !lessonDate.isAfter(RICH_SEED_TO);
    }

    private static List<Map<String, String>> readTable(String tableName) throws IOException {
        return InitialDataSqlReader.readTable(INITIAL_DATA, tableName);
    }

    private static void assertNoErrors(List<String> errors) {
        assertTrue(errors.isEmpty(), () -> INITIAL_DATA + System.lineSeparator() + String.join(System.lineSeparator(), errors));
    }
}
