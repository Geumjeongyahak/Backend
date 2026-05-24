package geumjeongyahak.domain.teacher_application.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import geumjeongyahak.domain.teacher_application.entity.TeacherApplication;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;

public interface TeacherApplicationRepository
    extends JpaRepository<TeacherApplication, Long>, JpaSpecificationExecutor<TeacherApplication> {

    boolean existsByApplicant_IdAndStatus(Long applicantId, TeacherApplicationStatus status);

    long countByStatus(TeacherApplicationStatus status);

    @EntityGraph(attributePaths = {"applicant", "preferredSubject", "preferredSubject.classroom", "reviewedBy"})
    Optional<TeacherApplication> findById(Long applicationId);

    @EntityGraph(attributePaths = {"applicant", "preferredSubject", "preferredSubject.classroom", "reviewedBy"})
    Optional<TeacherApplication> findFirstByApplicant_IdAndStatusInOrderByCreatedAtDesc(
        Long applicantId,
        Collection<TeacherApplicationStatus> statuses
    );
}
