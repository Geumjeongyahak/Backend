package geumjeongyahak.domain.base.service;

import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.student.repository.StudentRepository;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import geumjeongyahak.domain.teacher_application.repository.TeacherApplicationRepository;
import geumjeongyahak.domain.users.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ClassroomRepository classroomRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final AbsenceRequestRepository absenceRequestRepository;
    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherApplicationRepository teacherApplicationRepository;

    public AdminDashboardSummary getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return new AdminDashboardSummary(
            userRepository.count(),
            departmentRepository.count(),
            classroomRepository.count(),
            purchaseRequestRepository.countByStatus(PurchaseRequestStatus.PENDING),
            absenceRequestRepository.countByStatus(RequestStatus.PENDING),
            teacherApplicationRepository.countByStatus(TeacherApplicationStatus.PENDING),
            studentRepository.count(),
            subjectRepository.countByIsActiveTrue(),
            lessonRepository.countByIsDeletedFalse(),
            lessonRepository.countByIsDeletedFalseAndDate(today),
            lessonRepository.countByStatusAndIsDeletedFalseAndDateBetween(LessonStatus.SCHEDULED, weekStart, weekEnd),
            today,
            weekStart,
            weekEnd
        );
    }

    public record AdminDashboardSummary(
        long userCount,
        long departmentCount,
        long classroomCount,
        long pendingPurchaseRequestCount,
        long pendingAbsenceRequestCount,
        long pendingTeacherApplicationCount,
        long studentCount,
        long subjectCount,
        long lessonCount,
        long todayLessonCount,
        long weeklyScheduledLessonCount,
        LocalDate today,
        LocalDate weekStart,
        LocalDate weekEnd
    ) {
    }
}
