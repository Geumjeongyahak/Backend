package geumjeongyahak.domain.meeting_record.repository;

import geumjeongyahak.domain.meeting_record.entity.MeetingAbsenceReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAbsenceReportRepository extends JpaRepository<MeetingAbsenceReport, Long> {
    Optional<MeetingAbsenceReport> findByIdAndMeetingRecord_IdAndIsDeletedFalse(Long id, Long meetingRecordId);
}
