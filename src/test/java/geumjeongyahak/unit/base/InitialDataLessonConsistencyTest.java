package geumjeongyahak.unit.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("초기 데이터 수업 정합성 테스트")
class InitialDataLessonConsistencyTest {

    private static final Path DEVELOPMENT_INITIAL_DATA = Path.of("src/main/resources/sql/init_data.sql");
    private static final Pattern INSERT_PATTERN = Pattern.compile(
        "INSERT\\s+INTO\\s+(\\w+)\\s*\\((.*?)\\)\\s*VALUES\\s*(.*?);",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @ParameterizedTest(name = "{0}")
    @MethodSource("initialDataFiles")
    @DisplayName("수업은 연결된 과목의 교사, 교시, 시간, 요일, 운영 기간과 일치해야 한다")
    void lessons_matchSubjects(String description, Path initialDataFile) throws IOException {
        SeedData seedData = readSeedData(initialDataFile);
        List<String> errors = new ArrayList<>();
        requireRows(errors, "subjects", seedData.subjects());
        requireRows(errors, "lessons", seedData.lessons());

        for (LessonRow lesson : seedData.lessons().values()) {
            SubjectRow subject = seedData.subjects().get(lesson.subjectId());
            if (subject == null) {
                errors.add("lesson " + lesson.id() + ": subject " + lesson.subjectId() + "가 없습니다.");
                continue;
            }

            compare(errors, lesson.id(), "teacher_id", subject.teacherId(), lesson.teacherId());
            compare(errors, lesson.id(), "period", subject.period(), lesson.period());
            compare(errors, lesson.id(), "start_time", subject.startTime(), lesson.startTime());
            compare(errors, lesson.id(), "end_time", subject.endTime(), lesson.endTime());

            if (!lesson.date().getDayOfWeek().equals(subject.dayOfWeek())) {
                errors.add("lesson " + lesson.id() + ": date 요일이 subject와 다릅니다.");
            }
            if (lesson.date().isBefore(subject.startAt()) || lesson.date().isAfter(subject.endAt())) {
                errors.add("lesson " + lesson.id() + ": date가 subject 운영 기간 밖입니다.");
            }
        }

        assertNoErrors(initialDataFile, errors);
    }

    @Test
    @DisplayName("개발 초기 데이터의 하루 일정은 같은 날짜, 교사, 분반의 수업 묶음과 일치해야 한다")
    void dailySchedules_matchLessons() throws IOException {
        SeedData seedData = readSeedData(DEVELOPMENT_INITIAL_DATA);
        List<String> errors = new ArrayList<>();
        requireRows(errors, "subjects", seedData.subjects());
        requireRows(errors, "lessons", seedData.lessons());
        requireRows(errors, "daily_schedules", seedData.dailySchedules());

        for (DailyScheduleRow schedule : seedData.dailySchedules().values()) {
            List<LessonRow> matchedLessons = findLessons(seedData, schedule);
            if (matchedLessons.isEmpty()) {
                errors.add("daily_schedule " + schedule.id() + ": 연결할 수업이 없습니다.");
                continue;
            }

            LocalTime firstStartTime = matchedLessons.stream()
                .map(LessonRow::startTime)
                .min(LocalTime::compareTo)
                .orElseThrow();
            LocalTime lastEndTime = matchedLessons.stream()
                .map(LessonRow::endTime)
                .max(LocalTime::compareTo)
                .orElseThrow();

            compare(errors, schedule.id(), "activity_start_time", firstStartTime, schedule.activityStartTime());
            compare(errors, schedule.id(), "activity_end_time", lastEndTime, schedule.activityEndTime());
            matchedLessons.stream()
                .filter(lesson -> !lesson.status().equals(schedule.status()))
                .forEach(lesson -> errors.add(
                    "daily_schedule " + schedule.id() + ": lesson " + lesson.id() + "의 status가 다릅니다."
                ));
        }

        for (LessonRow lesson : seedData.lessons().values()) {
            boolean hasSchedule = seedData.dailySchedules().values().stream()
                .anyMatch(schedule -> matches(seedData, schedule, lesson));
            if (!hasSchedule) {
                errors.add("lesson " + lesson.id() + ": 연결할 daily_schedule이 없습니다.");
            }
        }

        assertNoErrors(DEVELOPMENT_INITIAL_DATA, errors);
    }

    private static void requireRows(List<String> errors, String table, Map<Long, ?> rows) {
        if (rows.isEmpty()) {
            errors.add(table + " 초기 데이터를 찾을 수 없습니다.");
        }
    }

    private static List<LessonRow> findLessons(SeedData seedData, DailyScheduleRow schedule) {
        return seedData.lessons().values().stream()
            .filter(lesson -> matches(seedData, schedule, lesson))
            .toList();
    }

    private static boolean matches(SeedData seedData, DailyScheduleRow schedule, LessonRow lesson) {
        SubjectRow subject = seedData.subjects().get(lesson.subjectId());
        return subject != null
            && schedule.classroomId().equals(subject.classroomId())
            && schedule.teacherId().equals(lesson.teacherId())
            && schedule.lessonDate().equals(lesson.date());
    }

    private static void compare(List<String> errors, Long rowId, String field, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            errors.add(
                "row " + rowId + ": " + field + "가 다릅니다. expected=" + expected + ", actual=" + actual
            );
        }
    }

    private static void assertNoErrors(Path initialDataFile, List<String> errors) {
        assertTrue(
            errors.isEmpty(),
            () -> initialDataFile + System.lineSeparator() + String.join(System.lineSeparator(), errors)
        );
    }

    private static SeedData readSeedData(Path initialDataFile) throws IOException {
        String sql = Files.readString(initialDataFile);
        Map<Long, SubjectRow> subjects = new LinkedHashMap<>();
        Map<Long, LessonRow> lessons = new LinkedHashMap<>();
        Map<Long, DailyScheduleRow> dailySchedules = new LinkedHashMap<>();

        Matcher matcher = INSERT_PATTERN.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase();
            List<String> columns = Arrays.stream(matcher.group(2).split(","))
                .map(String::trim)
                .toList();

            for (List<String> values : parseRows(matcher.group(3))) {
                Map<String, String> row = mapRow(columns, values);
                switch (table) {
                    case "subjects" -> {
                        SubjectRow subject = SubjectRow.from(row);
                        subjects.put(subject.id(), subject);
                    }
                    case "lessons" -> {
                        LessonRow lesson = LessonRow.from(row);
                        lessons.put(lesson.id(), lesson);
                    }
                    case "daily_schedules" -> {
                        DailyScheduleRow schedule = DailyScheduleRow.from(row);
                        dailySchedules.put(schedule.id(), schedule);
                    }
                    default -> {
                    }
                }
            }
        }

        return new SeedData(subjects, lessons, dailySchedules);
    }

    private static Map<String, String> mapRow(List<String> columns, List<String> values) {
        if (columns.size() != values.size()) {
            throw new IllegalArgumentException(
                "초기 데이터 INSERT 컬럼 수와 값 수가 다릅니다: " + columns.size() + " != " + values.size()
            );
        }

        Map<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            row.put(columns.get(index), normalize(values.get(index)));
        }
        return row;
    }

    private static List<List<String>> parseRows(String valuesClause) {
        List<List<String>> rows = new ArrayList<>();
        int rowStart = -1;
        int depth = 0;
        boolean quoted = false;

        for (int index = 0; index < valuesClause.length(); index++) {
            char current = valuesClause.charAt(index);
            if (current == '\'' && (index + 1 >= valuesClause.length() || valuesClause.charAt(index + 1) != '\'')) {
                quoted = !quoted;
            } else if (current == '\'' && quoted) {
                index++;
            } else if (!quoted && current == '(') {
                if (depth++ == 0) {
                    rowStart = index + 1;
                }
            } else if (!quoted && current == ')' && --depth == 0) {
                rows.add(splitValues(valuesClause.substring(rowStart, index)));
            }
        }
        return rows;
    }

    private static List<String> splitValues(String row) {
        List<String> values = new ArrayList<>();
        int valueStart = 0;
        boolean quoted = false;

        for (int index = 0; index < row.length(); index++) {
            char current = row.charAt(index);
            if (current == '\'' && (index + 1 >= row.length() || row.charAt(index + 1) != '\'')) {
                quoted = !quoted;
            } else if (current == '\'' && quoted) {
                index++;
            } else if (current == ',' && !quoted) {
                values.add(row.substring(valueStart, index).trim());
                valueStart = index + 1;
            }
        }
        values.add(row.substring(valueStart).trim());
        return values;
    }

    private static String normalize(String value) {
        String trimmed = value.trim();
        if ("NULL".equalsIgnoreCase(trimmed)) {
            return null;
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
        }
        return trimmed;
    }

    private static Stream<Object[]> initialDataFiles() {
        return Stream.of(
            new Object[]{"개발 초기 데이터", DEVELOPMENT_INITIAL_DATA},
            new Object[]{"테스트 초기 데이터", Path.of("src/test/resources/sql/init_data.sql")}
        );
    }

    private record SeedData(
        Map<Long, SubjectRow> subjects,
        Map<Long, LessonRow> lessons,
        Map<Long, DailyScheduleRow> dailySchedules
    ) {
    }

    private record SubjectRow(
        Long id,
        Long classroomId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        private static SubjectRow from(Map<String, String> row) {
            return new SubjectRow(
                longValue(row, "id"),
                longValue(row, "class_id"),
                longValue(row, "teacher_id"),
                LocalDate.parse(row.get("start_at")),
                LocalDate.parse(row.get("end_at")),
                DayOfWeek.valueOf(row.get("day_of_week")),
                LocalTime.parse(row.get("start_time")),
                LocalTime.parse(row.get("end_time")),
                integerValue(row, "period")
            );
        }
    }

    private record LessonRow(
        Long id,
        Long subjectId,
        Long teacherId,
        Integer period,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String status
    ) {
        private static LessonRow from(Map<String, String> row) {
            return new LessonRow(
                longValue(row, "id"),
                longValue(row, "subject_id"),
                longValue(row, "teacher_id"),
                integerValue(row, "period"),
                LocalDate.parse(row.get("date")),
                LocalTime.parse(row.get("start_time")),
                LocalTime.parse(row.get("end_time")),
                row.get("status")
            );
        }
    }

    private record DailyScheduleRow(
        Long id,
        Long classroomId,
        Long teacherId,
        LocalDate lessonDate,
        LocalTime activityStartTime,
        LocalTime activityEndTime,
        String status
    ) {
        private static DailyScheduleRow from(Map<String, String> row) {
            return new DailyScheduleRow(
                longValue(row, "id"),
                longValue(row, "classroom_id"),
                longValue(row, "teacher_id"),
                LocalDate.parse(row.get("lesson_date")),
                LocalTime.parse(row.get("activity_start_time")),
                LocalTime.parse(row.get("activity_end_time")),
                row.get("status")
            );
        }
    }

    private static Long longValue(Map<String, String> row, String column) {
        String value = row.get(column);
        return value == null ? null : Long.valueOf(value);
    }

    private static Integer integerValue(Map<String, String> row, String column) {
        String value = row.get(column);
        return value == null ? null : Integer.valueOf(value);
    }
}
