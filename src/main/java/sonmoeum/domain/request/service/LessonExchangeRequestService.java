package sonmoeum.domain.request.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.exception.LessonNotFoundException;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.request.entity.LessonExchangeRequest;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.exception.RequestAlreadyProcessedException;
import sonmoeum.domain.request.exception.RequestForbiddenException;
import sonmoeum.domain.request.exception.RequestNotFoundException;
import sonmoeum.domain.request.repository.LessonExchangeRequestRepository;
import sonmoeum.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import sonmoeum.domain.request.v1.dto.response.LessonExchangeRequestResponse;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonExchangeRequestService {

    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Transactional
    public LessonExchangeRequestResponse createLessonExchangeRequest(
        Long requesterId,
        CreateLessonExchangeRequestRequest request
    ) {
        log.debug("수업 교환 요청 생성 (requesterId={}, lessonId={})", requesterId, request.lessonId());

        Lesson lesson = lessonRepository.findByIdAndIsDeletedFalse(request.lessonId())
            .orElseThrow(() -> new LessonNotFoundException(request.lessonId()));

        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new UserNotFoundException(requesterId));

        LessonExchangeRequest exchangeRequest = new LessonExchangeRequest(
            lesson, requester, request.title(), request.content()
        );
        LessonExchangeRequest saved = lessonExchangeRequestRepository.save(exchangeRequest);

        log.debug("수업 교환 요청 생성 완료 (id={})", saved.getId());
        return LessonExchangeRequestResponse.from(saved);
    }

    public List<LessonExchangeRequestResponse> getLessonExchangeRequests(
        Long requesterId, boolean isAdmin, RequestStatus status
    ) {
        log.debug("수업 교환 요청 목록 조회 (isAdmin={}, status={})", isAdmin, status);

        List<LessonExchangeRequest> list;
        if (status != null) {
            list = isAdmin
                ? lessonExchangeRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                : lessonExchangeRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                    .stream()
                    .filter(r -> r.getRequestedBy().getId().equals(requesterId))
                    .toList();
        } else {
            list = isAdmin
                ? lessonExchangeRequestRepository.findAllByOrderByCreatedAtDesc()
                : lessonExchangeRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId);
        }

        return list.stream().map(LessonExchangeRequestResponse::from).toList();
    }

    public LessonExchangeRequestResponse getLessonExchangeRequest(
        Long requesterId, Long requestId, boolean isAdmin
    ) {
        log.debug("수업 교환 요청 상세 조회 (requestId={})", requestId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !exchangeRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        return LessonExchangeRequestResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestResponse approveLessonExchangeRequest(
        Long approverId, Long requestId, Long exchangeWithUserId
    ) {
        log.debug("수업 교환 요청 승인 (requestId={}, exchangeWithUserId={})", requestId, exchangeWithUserId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new UserNotFoundException(approverId));

        User newTeacher = userRepository.findById(exchangeWithUserId)
            .orElseThrow(() -> new UserNotFoundException(exchangeWithUserId));

        exchangeRequest.approve(approver);

        // 수업 담당 교사 변경
        exchangeRequest.getLesson().changeTeacher(newTeacher);

        log.debug("수업 교환 요청 승인 완료 (requestId={})", requestId);
        return LessonExchangeRequestResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestResponse rejectLessonExchangeRequest(
        Long approverId, Long requestId, String note
    ) {
        log.debug("수업 교환 요청 반려 (requestId={})", requestId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new UserNotFoundException(approverId));

        exchangeRequest.reject(approver, note);

        log.debug("수업 교환 요청 반려 완료 (requestId={})", requestId);
        return LessonExchangeRequestResponse.from(exchangeRequest);
    }
}
