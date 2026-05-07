package geumjeongyahak.domain.base.service;

import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.student.repository.StudentRepository;
import geumjeongyahak.domain.users.repository.UserRepository;
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
    private final StudentRepository studentRepository;

    public AdminDashboardSummary getSummary() {
        return new AdminDashboardSummary(
            userRepository.count(),
            departmentRepository.count(),
            classroomRepository.count(),
            purchaseRequestRepository.countByStatus(PurchaseRequestStatus.PENDING),
            studentRepository.count()
        );
    }

    public record AdminDashboardSummary(
        long userCount,
        long departmentCount,
        long classroomCount,
        long pendingPurchaseRequestCount,
        long studentCount
    ) {
    }
}
