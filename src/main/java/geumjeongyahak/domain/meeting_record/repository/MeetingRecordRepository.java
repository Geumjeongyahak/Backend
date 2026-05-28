package geumjeongyahak.domain.meeting_record.repository;

import geumjeongyahak.domain.meeting_record.entity.MeetingRecord;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRecordRepository extends JpaRepository<MeetingRecord, Long> {

    @EntityGraph(attributePaths = "author")
    @Query("""
        select r
        from MeetingRecord r
        where r.isDeleted = false
          and (:keyword is null or lower(r.title) like lower(concat('%', :keyword, '%')))
          and (:authorId is null or r.author.id = :authorId)
          and (:status is null or r.status = :status)
        """)
    Page<MeetingRecord> search(
        @Param("keyword") String keyword,
        @Param("authorId") Long authorId,
        @Param("status") MeetingRecordStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"author", "absenceReports", "absenceReports.author"})
    Optional<MeetingRecord> findByIdAndIsDeletedFalse(Long id);
}
