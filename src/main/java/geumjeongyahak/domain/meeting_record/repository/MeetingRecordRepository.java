package geumjeongyahak.domain.meeting_record.repository;

import geumjeongyahak.domain.meeting_record.entity.MeetingRecord;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MeetingRecordRepository extends JpaRepository<MeetingRecord, Long>, JpaSpecificationExecutor<MeetingRecord> {

    @EntityGraph(attributePaths = "author")
    Page<MeetingRecord> findAll(Specification<MeetingRecord> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "absenceReports", "absenceReports.author"})
    Optional<MeetingRecord> findByIdAndIsDeletedFalse(Long id);
}
