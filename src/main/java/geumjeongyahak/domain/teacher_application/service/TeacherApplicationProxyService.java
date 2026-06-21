package geumjeongyahak.domain.teacher_application.service;

import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import geumjeongyahak.domain.teacher_application.repository.TeacherApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherApplicationProxyService {

    private final TeacherApplicationRepository teacherApplicationRepository;

    public boolean existsPendingByApplicantId(Long applicantId) {
        return teacherApplicationRepository.existsByApplicant_IdAndStatus(
            applicantId,
            TeacherApplicationStatus.PENDING
        );
    }
}
