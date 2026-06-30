package geumjeongyahak.domain.meeting_record.repository;

import geumjeongyahak.domain.meeting_record.entity.MeetingRecordAttachment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRecordAttachmentRepository extends JpaRepository<MeetingRecordAttachment, Long> {

    boolean existsByMeetingRecordIdAndFileId(Long meetingRecordId, UUID fileId);

    long countByMeetingRecordId(Long meetingRecordId);

    Optional<MeetingRecordAttachment> findByMeetingRecordIdAndFileId(Long meetingRecordId, UUID fileId);
}
