package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonExchangeRequestAdminViewService {

    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;
    private final LessonExchangeProposalRepository lessonExchangeProposalRepository;

    public LessonExchangeDashboard getDashboard() {
        List<StatusCount<LessonExchangeRequestStatus>> requestStatusCounts = Arrays.stream(LessonExchangeRequestStatus.values())
            .map(status -> new StatusCount<>(
                status,
                requestStatusLabel(status),
                lessonExchangeRequestRepository.countByStatus(status)
            ))
            .toList();
        List<StatusCount<LessonExchangeProposalStatus>> proposalStatusCounts = Arrays.stream(LessonExchangeProposalStatus.values())
            .map(status -> new StatusCount<>(
                status,
                proposalStatusLabel(status),
                lessonExchangeProposalRepository.countByStatus(status)
            ))
            .toList();

        return new LessonExchangeDashboard(
            requestStatusCounts,
            proposalStatusCounts,
            countRequestStatus(requestStatusCounts, LessonExchangeRequestStatus.PENDING),
            countProposalStatus(proposalStatusCounts, LessonExchangeProposalStatus.ACTIVE),
            getReviewRequiredRequests()
        );
    }

    private List<ReviewRequiredRequestRow> getReviewRequiredRequests() {
        return lessonExchangeRequestRepository
            .findTop10ByStatusOrderByCreatedAtAsc(LessonExchangeRequestStatus.PENDING)
            .stream()
            .map(this::toReviewRequiredRequestRow)
            .toList();
    }

    private ReviewRequiredRequestRow toReviewRequiredRequestRow(LessonExchangeRequest request) {
        long proposalCount = lessonExchangeProposalRepository.countByRequest_Id(request.getId());
        long activeProposalCount = lessonExchangeProposalRepository.countByRequest_IdAndStatus(
            request.getId(),
            LessonExchangeProposalStatus.ACTIVE
        );

        return new ReviewRequiredRequestRow(
            request.getId(),
            request.getClassroomNameSnapshot(),
            request.getRequestedBy().getName(),
            request.getTitle(),
            request.getLessonDate(),
            request.getExpiresAt(),
            request.getCreatedAt(),
            request.getStatus(),
            requestStatusLabel(request.getStatus()),
            proposalCount,
            activeProposalCount
        );
    }

    private long countRequestStatus(
        List<StatusCount<LessonExchangeRequestStatus>> statusCounts,
        LessonExchangeRequestStatus status
    ) {
        return statusCounts.stream()
            .filter(statusCount -> statusCount.status() == status)
            .findFirst()
            .map(StatusCount::count)
            .orElse(0L);
    }

    private long countProposalStatus(
        List<StatusCount<LessonExchangeProposalStatus>> statusCounts,
        LessonExchangeProposalStatus status
    ) {
        return statusCounts.stream()
            .filter(statusCount -> statusCount.status() == status)
            .findFirst()
            .map(StatusCount::count)
            .orElse(0L);
    }

    private String requestStatusLabel(LessonExchangeRequestStatus status) {
        return switch (status) {
            case PENDING -> "승인 대기";
            case APPROVED -> "승인";
            case REJECTED -> "반려";
            case COMPLETED -> "교환 완료";
            case EXPIRED -> "만료";
            case CANCELLED -> "취소";
        };
    }

    private String proposalStatusLabel(LessonExchangeProposalStatus status) {
        return switch (status) {
            case ACTIVE -> "진행 중";
            case WITHDRAWN -> "철회";
            case ACCEPTED -> "수락";
            case CLOSED -> "종료";
        };
    }

    public record LessonExchangeDashboard(
        List<StatusCount<LessonExchangeRequestStatus>> requestStatusCounts,
        List<StatusCount<LessonExchangeProposalStatus>> proposalStatusCounts,
        long pendingRequestCount,
        long activeProposalCount,
        List<ReviewRequiredRequestRow> reviewRequiredRequests
    ) {
    }

    public record StatusCount<T extends Enum<T>>(
        T status,
        String label,
        long count
    ) {
    }

    public record ReviewRequiredRequestRow(
        Long id,
        String classroomName,
        String requestedByName,
        String title,
        LocalDate lessonDate,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LessonExchangeRequestStatus status,
        String statusLabel,
        long proposalCount,
        long activeProposalCount
    ) {
    }
}
