package geumjeongyahak.domain.meeting_record.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.meeting_record.entity.MeetingAbsenceReport;
import geumjeongyahak.domain.meeting_record.entity.MeetingRecord;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import geumjeongyahak.domain.meeting_record.exception.MeetingRecordErrorCode;
import geumjeongyahak.domain.meeting_record.repository.MeetingAbsenceReportRepository;
import geumjeongyahak.domain.meeting_record.repository.MeetingRecordRepository;
import geumjeongyahak.domain.meeting_record.repository.specification.MeetingRecordSpecs;
import geumjeongyahak.domain.meeting_record.v1.dto.request.CreateAbsenceReportRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.CreateMeetingRecordRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.MeetingRecordSearchRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.UpdateAbsenceReportRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.UpdateMeetingRecordRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingAbsenceReportResponse;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingRecordDetailResponse;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingRecordSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingRecordService {

    private final MeetingRecordRepository meetingRecordRepository;
    private final MeetingAbsenceReportRepository absenceReportRepository;
    private final UserProxyService userProxyService;

    public PaginationResponse<MeetingRecordSummaryResponse> getMeetingRecords(
        Long requesterId,
        MeetingRecordSearchRequest request
    ) {
        User requester = getStaffUser(requesterId);
        Long authorId = Boolean.TRUE.equals(request.getMineOnly()) ? requester.getId() : null;
        Specification<MeetingRecord> spec = Specification.allOf(
            MeetingRecordSpecs.isNotDeleted(),
            MeetingRecordSpecs.containsTitle(request.getKeyword()),
            MeetingRecordSpecs.hasAuthorId(authorId),
            MeetingRecordSpecs.hasStatus(request.getStatus())
        );

        return PaginationResponse.from(
            meetingRecordRepository.findAll(spec, request.toRequest()),
            MeetingRecordSummaryResponse::from
        );
    }

    @Transactional
    public MeetingRecordDetailResponse createMeetingRecord(Long requesterId, CreateMeetingRecordRequest request) {
        User author = getStaffUser(requesterId);
        MeetingRecord saved = meetingRecordRepository.save(
            new MeetingRecord(author, requireText(request.title()), requireText(request.agenda()))
        );
        return MeetingRecordDetailResponse.from(saved);
    }

    @Transactional
    public MeetingRecordDetailResponse getMeetingRecord(Long requesterId, Long recordId) {
        getStaffUser(requesterId);
        MeetingRecord record = getActiveRecord(recordId);
        record.incrementViewCount();
        return MeetingRecordDetailResponse.from(record);
    }

    public MeetingRecordDetailResponse getMeetingRecordWithoutViewCount(Long requesterId, Long recordId) {
        getStaffUser(requesterId);
        return MeetingRecordDetailResponse.from(getActiveRecord(recordId));
    }

    @Transactional
    public MeetingRecordDetailResponse updateMeetingRecord(
        Long requesterId,
        Long recordId,
        UpdateMeetingRecordRequest request,
        boolean isAdmin
    ) {
        getStaffUser(requesterId);
        MeetingRecord record = getActiveRecord(recordId);
        assertRecordOwnerOrAdmin(record, requesterId, isAdmin);
        assertValidStatusTransition(record, request.status());

        record.update(
            normalizeRequiredIfPresent(request.title()),
            normalizeRequiredIfPresent(request.agenda()),
            request.discussion(),
            request.suggestion(),
            request.status()
        );
        return MeetingRecordDetailResponse.from(record);
    }

    @Transactional
    public void deleteMeetingRecord(Long requesterId, Long recordId, boolean isAdmin) {
        getStaffUser(requesterId);
        MeetingRecord record = getActiveRecord(recordId);
        assertRecordOwnerOrAdmin(record, requesterId, isAdmin);
        record.delete();
    }

    @Transactional
    public MeetingAbsenceReportResponse createAbsenceReport(
        Long requesterId,
        Long recordId,
        CreateAbsenceReportRequest request
    ) {
        User author = getStaffUser(requesterId);
        MeetingRecord record = getActiveRecord(recordId);
        assertBeforeMeeting(record);
        MeetingAbsenceReport report = new MeetingAbsenceReport(
            author,
            requireText(request.reason()),
            request.opinion()
        );
        record.addAbsenceReport(report);
        absenceReportRepository.save(report);
        return MeetingAbsenceReportResponse.from(report);
    }

    @Transactional
    public MeetingAbsenceReportResponse updateAbsenceReport(
        Long requesterId,
        Long recordId,
        Long reportId,
        UpdateAbsenceReportRequest request
    ) {
        getStaffUser(requesterId);
        MeetingRecord record = getActiveRecord(recordId);
        assertBeforeMeeting(record);

        MeetingAbsenceReport report = getActiveReport(recordId, reportId);
        assertReportOwner(report, requesterId);
        report.update(requireText(request.reason()), request.opinion());
        return MeetingAbsenceReportResponse.from(report);
    }

    @Transactional
    public void deleteAbsenceReport(Long requesterId, Long recordId, Long reportId) {
        getStaffUser(requesterId);
        MeetingRecord record = getActiveRecord(recordId);
        assertBeforeMeeting(record);

        MeetingAbsenceReport report = getActiveReport(recordId, reportId);
        assertReportOwner(report, requesterId);
        report.delete();
    }

    public MeetingRecordStatus[] getStatuses() {
        return MeetingRecordStatus.values();
    }

    private MeetingRecord getActiveRecord(Long recordId) {
        return meetingRecordRepository.findByIdAndIsDeletedFalse(recordId)
            .orElseThrow(() -> new BusinessException(MeetingRecordErrorCode.NOT_FOUND));
    }

    private MeetingAbsenceReport getActiveReport(Long recordId, Long reportId) {
        return absenceReportRepository.findByIdAndMeetingRecord_IdAndIsDeletedFalse(reportId, recordId)
            .orElseThrow(() -> new BusinessException(MeetingRecordErrorCode.ABSENCE_REPORT_NOT_FOUND));
    }

    private User getStaffUser(Long userId) {
        User user = userProxyService.getById(userId);
        if (user.getRole() == RoleType.GUEST) {
            throw new BusinessException(MeetingRecordErrorCode.STAFF_ONLY);
        }
        return user;
    }

    private void assertRecordOwnerOrAdmin(MeetingRecord record, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !record.getAuthor().getId().equals(requesterId)) {
            throw new BusinessException(MeetingRecordErrorCode.FORBIDDEN);
        }
    }

    private void assertReportOwner(MeetingAbsenceReport report, Long requesterId) {
        if (!report.getAuthor().getId().equals(requesterId)) {
            throw new BusinessException(MeetingRecordErrorCode.FORBIDDEN);
        }
    }

    private void assertBeforeMeeting(MeetingRecord record) {
        if (record.getStatus() != MeetingRecordStatus.BEFORE_MEETING) {
            throw new BusinessException(MeetingRecordErrorCode.INVALID_STATUS);
        }
    }

    private void assertValidStatusTransition(MeetingRecord record, MeetingRecordStatus nextStatus) {
        if (nextStatus == null || nextStatus == record.getStatus()) {
            return;
        }
        if (record.getStatus() == MeetingRecordStatus.AFTER_MEETING && nextStatus == MeetingRecordStatus.BEFORE_MEETING) {
            throw new BusinessException(MeetingRecordErrorCode.INVALID_STATUS);
        }
    }

    private String normalizeRequiredIfPresent(String value) {
        return value == null ? null : requireText(value);
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(MeetingRecordErrorCode.INVALID_INPUT);
        }
        return value.trim();
    }
}
