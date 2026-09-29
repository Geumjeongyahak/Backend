package geumjeongyahak.domain.meeting_record.repository;

import geumjeongyahak.domain.meeting_record.entity.MeetingRecord;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRecordRepository extends JpaRepository<MeetingRecord, Long>, JpaSpecificationExecutor<MeetingRecord> {

    @EntityGraph(attributePaths = "author")
    Page<MeetingRecord> findAll(Specification<MeetingRecord> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "absenceReports", "absenceReports.author"})
    Optional<MeetingRecord> findByIdAndIsDeletedFalse(Long id);

    // 동시 조회에도 증가분이 사라지지 않고, 회의록 전체 저장과 updated_at 변경이 없다
    @Modifying
    @Query("update MeetingRecord r set r.viewCount = r.viewCount + 1 where r.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
