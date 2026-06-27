package geumjeongyahak.domain.meeting_record.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.meeting_record.service.MeetingRecordService;
import geumjeongyahak.domain.meeting_record.v1.dto.request.AttachMeetingRecordFileRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.CreateAbsenceReportRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.CreateMeetingRecordRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.MeetingRecordSearchRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.UpdateAbsenceReportRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.UpdateMeetingRecordRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingAbsenceReportResponse;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingRecordDetailResponse;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingRecordSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/meeting-records")
@RequiredArgsConstructor
@Tag(name = "MeetingRecord", description = "교학 회의록 API")
public class MeetingRecordController {

    private static final String STAFF_ONLY = "hasAnyRole('ADMIN','MANAGER','VOLUNTEER')";

    private final MeetingRecordService meetingRecordService;

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "교학 회의록 목록 조회")
    @GetMapping
    public ResponseEntity<PaginationResponse<MeetingRecordSummaryResponse>> getMeetingRecords(
        @Valid @ModelAttribute MeetingRecordSearchRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/meeting-records keyword={} mineOnly={}", request.getKeyword(), request.getMineOnly());
        return ResponseEntity.ok(meetingRecordService.getMeetingRecords(userDetails.getUserId(), request));
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "교학 회의록 생성")
    @PostMapping
    public ResponseEntity<MeetingRecordDetailResponse> createMeetingRecord(
        @Valid @RequestBody CreateMeetingRecordRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/meeting-records");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(meetingRecordService.createMeetingRecord(userDetails.getUserId(), request));
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "교학 회의록 상세 조회")
    @GetMapping("/{recordId}")
    public ResponseEntity<MeetingRecordDetailResponse> getMeetingRecord(
        @PathVariable Long recordId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/meeting-records/{}", recordId);
        return ResponseEntity.ok(meetingRecordService.getMeetingRecord(userDetails.getUserId(), recordId));
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "교학 회의록 수정 및 회의 후 전환")
    @PatchMapping("/{recordId}")
    public ResponseEntity<MeetingRecordDetailResponse> updateMeetingRecord(
        @PathVariable Long recordId,
        @Valid @RequestBody UpdateMeetingRecordRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/meeting-records/{}", recordId);
        return ResponseEntity.ok(
            meetingRecordService.updateMeetingRecord(userDetails.getUserId(), recordId, request, userDetails.isAdmin())
        );
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "교학 회의록 삭제")
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteMeetingRecord(
        @PathVariable Long recordId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/meeting-records/{}", recordId);
        meetingRecordService.deleteMeetingRecord(userDetails.getUserId(), recordId, userDetails.isAdmin());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "교학 회의록 첨부파일 업로드 및 연동")
    @PostMapping(value = "/{recordId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> attachUploadedAttachment(
        @PathVariable Long recordId,
        @RequestPart("file") MultipartFile file,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/meeting-records/{}/attachments multipart", recordId);
        return ResponseEntity.ok(
            meetingRecordService.attachUploadedAttachment(userDetails.getUserId(), recordId, file, userDetails.isAdmin())
        );
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "등록된 파일을 교학 회의록 첨부파일로 연동")
    @PostMapping(value = "/{recordId}/attachments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileUploadResponse> attachRegisteredAttachment(
        @PathVariable Long recordId,
        @Valid @RequestBody AttachMeetingRecordFileRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/meeting-records/{}/attachments json", recordId);
        return ResponseEntity.ok(meetingRecordService.attachRegisteredAttachment(
            userDetails.getUserId(),
            recordId,
            request.fileId(),
            request.sortOrder(),
            userDetails.isAdmin()
        ));
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "교학 회의록 첨부파일 삭제")
    @DeleteMapping("/{recordId}/attachments/{fileId}")
    public ResponseEntity<Void> detachAttachment(
        @PathVariable Long recordId,
        @PathVariable UUID fileId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/meeting-records/{}/attachments/{}", recordId, fileId);
        meetingRecordService.detachAttachment(userDetails.getUserId(), recordId, fileId, userDetails.isAdmin());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "불참 사유서 추가")
    @PostMapping("/{recordId}/absence-reports")
    public ResponseEntity<MeetingAbsenceReportResponse> createAbsenceReport(
        @PathVariable Long recordId,
        @Valid @RequestBody CreateAbsenceReportRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/meeting-records/{}/absence-reports", recordId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(meetingRecordService.createAbsenceReport(userDetails.getUserId(), recordId, request));
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "불참 사유서 수정")
    @PatchMapping("/{recordId}/absence-reports/{absenceReportId}")
    public ResponseEntity<MeetingAbsenceReportResponse> updateAbsenceReport(
        @PathVariable Long recordId,
        @PathVariable Long absenceReportId,
        @Valid @RequestBody UpdateAbsenceReportRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/meeting-records/{}/absence-reports/{}", recordId, absenceReportId);
        return ResponseEntity.ok(
            meetingRecordService.updateAbsenceReport(userDetails.getUserId(), recordId, absenceReportId, request)
        );
    }

    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "불참 사유서 삭제")
    @DeleteMapping("/{recordId}/absence-reports/{absenceReportId}")
    public ResponseEntity<Void> deleteAbsenceReport(
        @PathVariable Long recordId,
        @PathVariable Long absenceReportId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/meeting-records/{}/absence-reports/{}", recordId, absenceReportId);
        meetingRecordService.deleteAbsenceReport(userDetails.getUserId(), recordId, absenceReportId);
        return ResponseEntity.noContent().build();
    }
}
