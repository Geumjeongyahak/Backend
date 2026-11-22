package geumjeongyahak.domain.meeting_record.service.access;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.service.access.AttachmentReadPolicy;
import geumjeongyahak.domain.meeting_record.repository.MeetingRecordAttachmentRepository;
import lombok.RequiredArgsConstructor;

/**
 * 회의록 첨부: 삭제되지 않은 회의록에 붙어 있으면 ADMIN·MANAGER·VOLUNTEER.
 * {@code MeetingRecordController.STAFF_ONLY}({@code hasAnyRole('ADMIN','MANAGER','VOLUNTEER')})와 같아야 한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingRecordAttachmentReadPolicy implements AttachmentReadPolicy {

    private static final Set<String> STAFF_ROLES = Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VOLUNTEER");

    private final MeetingRecordAttachmentRepository meetingRecordAttachmentRepository;

    @Override
    public boolean canRead(UUID fileId, CustomUserDetails user) {
        return user != null
            && user.getAuthorities().stream().anyMatch(a -> STAFF_ROLES.contains(a.getAuthority()))
            && meetingRecordAttachmentRepository.existsByFileIdAndMeetingRecordIsDeletedFalse(fileId);
    }
}
