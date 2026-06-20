package geumjeongyahak.unit.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.domain.base.service.AdminDashboardService;
import geumjeongyahak.domain.base.service.AdminDashboardService.AdminDashboardSummary;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.student.repository.StudentRepository;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import geumjeongyahak.domain.teacher_application.repository.TeacherApplicationRepository;
import geumjeongyahak.domain.event.repository.EventRepository;
import geumjeongyahak.domain.users.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private AbsenceRequestRepository absenceRequestRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherApplicationRepository teacherApplicationRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void getSummary_includesLessonCounts() {
        given(userRepository.countByIsDeletedFalse()).willReturn(4L);
        given(departmentRepository.count()).willReturn(6L);
        given(classroomRepository.count()).willReturn(9L);
        given(purchaseRequestRepository.countByStatus(PurchaseRequestStatus.PENDING)).willReturn(2L);
        given(absenceRequestRepository.countByStatus(RequestStatus.PENDING)).willReturn(5L);
        given(teacherApplicationRepository.countByStatus(TeacherApplicationStatus.PENDING)).willReturn(13L);
        given(studentRepository.count()).willReturn(11L);
        given(subjectRepository.countByIsActiveTrue()).willReturn(7L);
        given(lessonRepository.countByIsDeletedFalse()).willReturn(20L);
        given(lessonRepository.countByIsDeletedFalseAndDate(any(LocalDate.class))).willReturn(3L);
        given(lessonRepository.countByStatusAndIsDeletedFalseAndDateBetween(
            eq(LessonStatus.SCHEDULED),
            any(LocalDate.class),
            any(LocalDate.class)
        )).willReturn(8L);
        given(eventRepository.countByIsDeletedFalseAndEventDateGreaterThanEqual(any(LocalDate.class))).willReturn(12L);

        AdminDashboardSummary summary = adminDashboardService.getSummary();

        assertThat(summary.userCount()).isEqualTo(4L);
        assertThat(summary.departmentCount()).isEqualTo(6L);
        assertThat(summary.classroomCount()).isEqualTo(9L);
        assertThat(summary.pendingPurchaseRequestCount()).isEqualTo(2L);
        assertThat(summary.pendingAbsenceRequestCount()).isEqualTo(5L);
        assertThat(summary.pendingTeacherApplicationCount()).isEqualTo(13L);
        assertThat(summary.studentCount()).isEqualTo(11L);
        assertThat(summary.subjectCount()).isEqualTo(7L);
        assertThat(summary.lessonCount()).isEqualTo(20L);
        assertThat(summary.todayLessonCount()).isEqualTo(3L);
        assertThat(summary.weeklyScheduledLessonCount()).isEqualTo(8L);
        assertThat(summary.upcomingEventCount()).isEqualTo(12L);
        assertThat(summary.today()).isNotNull();
        assertThat(summary.weekStart()).isNotNull();
        assertThat(summary.weekEnd()).isNotNull();
    }
}
