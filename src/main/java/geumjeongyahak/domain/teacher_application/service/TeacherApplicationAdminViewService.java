package geumjeongyahak.domain.teacher_application.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.teacher_application.enums.TeacherApplicationStatus;
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
    private final ClassroomProxyService classroomProxyService;

    public TeacherApplicationPage getTeacherApplications(TeacherApplicationFilter filter) {
        TeacherApplicationPaginationRequest request = new TeacherApplicationPaginationRequest();
        request.setPage(filter.page());
        request.setSize(filter.size());
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
            getActiveClassrooms()
        );
    }

    public List<StatusOption> getStatusOptions() {
        return Arrays.stream(TeacherApplicationStatus.values())
            .map(status -> new StatusOption(status, statusLabel(status)))
            .toList();
    }

    public List<ClassroomOption> getActiveClassrooms() {
        return classroomProxyService.getActiveClassroomsOrderByName()
            .stream()
            .map(this::toClassroomOption)
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
        Long classroomId,
        LocalDate teacherStartAt,
        LocalDate teacherEndAt,
        String note
    ) {
        teacherApplicationService.approveTeacherApplication(
            reviewerId,
            applicationId,
            new ApproveTeacherApplicationRequest(classroomId, teacherStartAt, teacherEndAt, note)
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

    private ClassroomOption toClassroomOption(Classroom classroom) {
        return new ClassroomOption(
            classroom.getId(),
            classroom.getName()
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

    public record TeacherApplicationFilter(
        TeacherApplicationStatus status,
        String keyword,
        Integer page,
        Integer size
    ) {
    }

    public record TeacherApplicationPage(
        AdminPage<TeacherApplicationRow> applications,
        TeacherApplicationFilter filter,
        List<StatusOption> statusOptions
    ) {
    }

    public record TeacherApplicationRow(
        TeacherApplicationResponse application,
        String statusLabel
    ) {
    }

    public record TeacherApplicationDetail(
        TeacherApplicationResponse application,
        String statusLabel,
        boolean pending,
        List<ClassroomOption> classroomOptions
    ) {
    }

    public record StatusOption(
        TeacherApplicationStatus status,
        String label
    ) {
    }

    public record ClassroomOption(
        Long id,
        String name
    ) {
    }
}
