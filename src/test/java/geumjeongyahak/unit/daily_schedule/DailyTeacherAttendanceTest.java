package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.users.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DailyTeacherAttendanceTest {

    @Test
    void checkOut_recordsCheckOutTime() {
        DailyTeacherAttendance attendance = teacherAttendance();
        LocalDateTime attendedAt = LocalDateTime.of(2026, 6, 27, 18, 30);
        LocalDateTime checkedOutAt = LocalDateTime.of(2026, 6, 27, 21, 10);

        attendance.updateAttendance(DailyTeacherAttendanceStatus.PRESENT, attendedAt, null, null);
        attendance.checkOut(checkedOutAt);

        assertThat(attendance.isAttended()).isTrue();
        assertThat(attendance.isCheckedOut()).isTrue();
        assertThat(attendance.getCheckedOutAt()).isEqualTo(checkedOutAt);
    }

    @Test
    void updateAttendance_clearsCheckOutTimeWhenAttendanceTimeIsCleared() {
        DailyTeacherAttendance attendance = teacherAttendance();
        attendance.updateAttendance(
            DailyTeacherAttendanceStatus.PRESENT,
            LocalDateTime.of(2026, 6, 27, 18, 30),
            null,
            null
        );
        attendance.checkOut(LocalDateTime.of(2026, 6, 27, 21, 10));

        attendance.updateAttendance(DailyTeacherAttendanceStatus.ABSENT, null, null, null);

        assertThat(attendance.isAttended()).isFalse();
        assertThat(attendance.isCheckedOut()).isFalse();
        assertThat(attendance.getCheckedOutAt()).isNull();
    }

    private DailyTeacherAttendance teacherAttendance() {
        Classroom classroom = Classroom.builder()
            .name("장미반")
            .type(ClassroomType.WEEKDAY)
            .build();
        ReflectionTestUtils.setField(classroom, "id", 1L);
        User teacher = User.builder()
            .name("홍길동")
            .role(RoleType.VOLUNTEER)
            .build();
        ReflectionTestUtils.setField(teacher, "id", 2L);
        DailySchedule dailySchedule = new DailySchedule(
            classroom,
            teacher,
            LocalDate.of(2026, 6, 27),
            LocalTime.of(18, 30),
            LocalTime.of(21, 10)
        );
        ReflectionTestUtils.setField(dailySchedule, "id", 100L);
        return new DailyTeacherAttendance(dailySchedule, 160);
    }
}
