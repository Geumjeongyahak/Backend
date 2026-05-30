package geumjeongyahak.domain.meeting_record.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import geumjeongyahak.domain.meeting_record.v1.dto.request.CreateMeetingRecordRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.MeetingRecordSearchRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.UpdateMeetingRecordRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingRecordDetailResponse;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingRecordSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingRecordAdminViewService {

    private final MeetingRecordService meetingRecordService;

    public AdminPage<MeetingRecordSummaryResponse> getMeetingRecords(MeetingRecordFilter filter) {
        MeetingRecordSearchRequest request = new MeetingRecordSearchRequest();
        request.setKeyword(filter.keyword());
        request.setStatus(filter.status());
        request.setMineOnly(false);
        request.setPage(filter.page() == null ? 0 : filter.page());
        request.setSize(filter.size() == null ? 10 : filter.size());

        PaginationResponse<MeetingRecordSummaryResponse> response =
            meetingRecordService.getMeetingRecords(filter.requesterId(), request);
        return AdminPage.from(response);
    }

    public MeetingRecordStatus[] getStatuses() {
        return meetingRecordService.getStatuses();
    }

    public MeetingRecordDetailResponse getMeetingRecord(Long requesterId, Long recordId) {
        return meetingRecordService.getMeetingRecord(requesterId, recordId);
    }

    public MeetingRecordDetailResponse getMeetingRecordForEdit(Long requesterId, Long recordId) {
        return meetingRecordService.getMeetingRecordWithoutViewCount(requesterId, recordId);
    }

    @Transactional
    public Long createMeetingRecord(Long requesterId, String title, String agenda) {
        return meetingRecordService.createMeetingRecord(
            requesterId,
            new CreateMeetingRecordRequest(title, agenda)
        ).id();
    }

    @Transactional
    public void updateMeetingRecord(
        Long requesterId,
        Long recordId,
        String title,
        String agenda,
        String discussion,
        String suggestion,
        MeetingRecordStatus status
    ) {
        meetingRecordService.updateMeetingRecord(
            requesterId,
            recordId,
            new UpdateMeetingRecordRequest(title, agenda, discussion, suggestion, status),
            true
        );
    }

    @Transactional
    public void deleteMeetingRecord(Long requesterId, Long recordId) {
        meetingRecordService.deleteMeetingRecord(requesterId, recordId, true);
    }

    public record MeetingRecordFilter(
        Long requesterId,
        MeetingRecordStatus status,
        String keyword,
        Integer page,
        Integer size
    ) {
    }
}
