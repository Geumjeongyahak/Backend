package geumjeongyahak.domain.teacher_application.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
import geumjeongyahak.domain.teacher_application.service.dto.TeacherApplicationDetail;
import geumjeongyahak.domain.teacher_application.service.dto.TeacherApplicationFilter;
import geumjeongyahak.domain.teacher_application.service.dto.TeacherApplicationPage;
import geumjeongyahak.domain.teacher_application.service.dto.TeacherApplicationRow;
import geumjeongyahak.domain.teacher_application.service.dto.TeacherApplicationStatusOption;
import geumjeongyahak.domain.teacher_application.service.dto.TeacherApplicationSubjectOption;
import geumjeongyahak.domain.teacher_application.v1.dto.request.ApproveTeacherApplicationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.request.RejectTeacherApplicationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.request.TeacherApplicationPaginationRequest;
import geumjeongyahak.domain.teacher_application.v1.dto.response.TeacherApplicationResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherApplicationAdminViewService {

    private final TeacherApplicationService teacherApplicationService;
    private final SubjectProxyService subjectProxyService;

    public TeacherApplicationPage getTeacherApplications(TeacherApplicationFilter filter) {
        TeacherApplicationPaginationRequest request = new TeacherApplicationPaginationRequest();
        if (filter.page() != null) {
            request.setPage(filter.page());
        }
        if (filter.size() != null) {
            request.setSize(filter.size());
        }
        request.setKeyword(filter.keyword());

        PaginationResponse<TeacherApplicationResponse> response =
            teacherApplicationService.getTeacherApplications(filter.status(), request);
        AdminPage<TeacherApplicationResponse> sourcePage = AdminPage.from(response);
        AdminPage<TeacherApplicationRow> page = new AdminPage<>(
            sourcePage.content().stream()
                .map(this::toRow)
                .toList(),
            sourcePage.page(),
            sourcePage.size(),
            sourcePage.totalElements(),
            sourcePage.totalPages()
        );

        return new TeacherApplicationPage(
            page,
            filter,
            getStatusOptions()
        );
    }

    public TeacherApplicationDetail getTeacherApplication(Long applicationId) {
        TeacherApplicationResponse application = teacherApplicationService.getTeacherApplication(applicationId);

        return new TeacherApplicationDetail(
            application,
            statusLabel(application.status()),
            application.status() == TeacherApplicationStatus.PENDING,
            getAssignableSubjects()
        );
    }

    public List<TeacherApplicationStatusOption> getStatusOptions() {
        return Arrays.stream(TeacherApplicationStatus.values())
            .map(status -> new TeacherApplicationStatusOption(status, statusLabel(status)))
            .toList();
    }

    public List<TeacherApplicationSubjectOption> getAssignableSubjects() {
        return subjectProxyService.getUnassignedActiveSubjectsOrderByStartAtAndId()
            .stream()
            .map(SubjectDetailResponse::from)
            .map(this::toSubjectOption)
            .toList();
    }

    public long getPendingCount() {
        TeacherApplicationPaginationRequest request = new TeacherApplicationPaginationRequest();
        request.setPage(0);
        request.setSize(1);

        return teacherApplicationService
            .getTeacherApplications(TeacherApplicationStatus.PENDING, request)
            .getTotalElements();
    }

    @Transactional
    public void approve(
        Long reviewerId,
        Long applicationId,
        List<Long> assignedSubjectIds,
        LocalDate teacherStartAt,
        LocalDate teacherEndAt,
        String note
    ) {
        teacherApplicationService.approveTeacherApplication(
            reviewerId,
            applicationId,
            new ApproveTeacherApplicationRequest(assignedSubjectIds, teacherStartAt, teacherEndAt, note)
        );
    }

    @Transactional
    public void reject(Long reviewerId, Long applicationId, String note) {
        teacherApplicationService.rejectTeacherApplication(
            reviewerId,
            applicationId,
            new RejectTeacherApplicationRequest(note)
        );
    }

    private TeacherApplicationRow toRow(TeacherApplicationResponse application) {
        return new TeacherApplicationRow(
            application,
            statusLabel(application.status())
        );
    }

    private TeacherApplicationSubjectOption toSubjectOption(SubjectDetailResponse subject) {
        return new TeacherApplicationSubjectOption(
            subject.id(),
            subject.name(),
            subject.classroomName(),
            subject.dayOfWeek(),
            subject.startTime(),
            subject.endTime()
        );
    }

    private String statusLabel(TeacherApplicationStatus status) {
        return switch (status) {
            case PENDING -> "승인 대기";
            case APPROVED -> "승인";
            case REJECTED -> "반려";
            case CANCELLED -> "취소";
        };
    }
}
