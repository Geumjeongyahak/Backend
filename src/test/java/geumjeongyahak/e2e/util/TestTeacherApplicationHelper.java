package geumjeongyahak.e2e.util;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestTeacherApplicationHelper {

    private final JdbcTemplate jdbcTemplate;

    public TestTeacherApplicationHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void cleanupApplications() {
        jdbcTemplate.update("DELETE FROM teacher_schedule_assignments");
        jdbcTemplate.update("DELETE FROM teacher_applications");
    }

    public void cleanupSubjects(long... subjectIds) {
        for (long subjectId : subjectIds) {
            jdbcTemplate.update("DELETE FROM teacher_schedule_assignments WHERE subject_id = ?", subjectId);
            jdbcTemplate.update("DELETE FROM lessons WHERE subject_id = ?", subjectId);
            jdbcTemplate.update("DELETE FROM subjects WHERE id = ?", subjectId);
        }
    }

    public void insertUnassignedSubject(
        long subjectId,
        long classroomId,
        String name,
        String startAt,
        String endAt,
        String dayOfWeek,
        String startTime,
        String endTime,
        int period
    ) {
        jdbcTemplate.update("""
            INSERT INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, NULL, '교원 신청/배정 E2E 테스트 과목', TRUE)
            """, subjectId, classroomId, name, startAt, endAt, dayOfWeek, startTime, endTime, period);
    }

    public void insertTeacherApplication(long id, long applicantId, long preferredSubjectId, String status) {
        LocalDateTime createdAt = LocalDateTime.parse("2026-05-20T10:00:00");
        boolean reviewed = !"PENDING".equals(status) && !"CANCELLED".equals(status);
        jdbcTemplate.update("""
            INSERT INTO teacher_applications (
                id, applicant_id, applicant_name, applicant_phone_number, applicant_email,
                birth_date, address, education_and_major, preferred_subject_id,
                motivation, desired_teacher_image, meaning_of_sharing,
                status, reviewed_at, reviewed_by, review_note, created_at, updated_at
            )
            VALUES (?, ?, '이영희', '010-1234-5678', 'guest01@test.com',
                    '1999-03-15', '부산광역시 금정구', '부산대학교 국어국문학과 졸업', ?,
                    '지원 동기', '희망하는 교사상', '나눔의 의미',
                    ?, ?, ?, ?, ?, ?)
            """,
            id,
            applicantId,
            preferredSubjectId,
            status,
            reviewed ? createdAt : null,
            reviewed ? 1L : null,
            reviewed ? "상태 fixture" : null,
            createdAt,
            createdAt
        );
    }

    public void resetApplicant(long userId) {
        jdbcTemplate.update("""
            UPDATE users
            SET role = 'GUEST',
                classroom_id = NULL,
                teacher_start_at = NULL,
                teacher_end_at = NULL
            WHERE id = ?
            """, userId);
        jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = ? AND permission_code LIKE 'channel:write:%'", userId);
    }
}
