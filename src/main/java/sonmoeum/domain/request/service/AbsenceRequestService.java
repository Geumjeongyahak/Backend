package sonmoeum.domain.request.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.enums.TeacherAttendanceStatus;
import sonmoeum.domain.lesson.exception.LessonNotFoundException;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.request.entity.AbsenceRequest;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.exception.RequestAlreadyProcessedException;
import sonmoeum.domain.request.exception.RequestForbiddenException;
import sonmoeum.domain.request.exception.RequestNotFoundException;
import sonmoeum.domain.request.repository.AbsenceRequestRepository;
import sonmoeum.domain.request.v1.dto.request.CreateAbsenceRequestRequest;
import sonmoeum.domain.request.v1.dto.response.AbsenceRequestResponse;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbsenceRequestService {

    private final AbsenceRequestRepository absenceRequestRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Transactional
    public AbsenceRequestResponse createAbsenceRequest(Long requesterId, CreateAbsenceRequestRequest request) {
        log.debug("결석 요청 생성 (requesterId={}, lessonId={})", requesterId, request.lessonId());

        Lesson lesson = lessonRepository.findByIdAndIsDeletedFalse(request.lessonId())
            .orElseThrow(() -> new LessonNotFoundException(request.lessonId()));

        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new UserNotFoundException(requesterId));

        AbsenceRequest absenceRequest = new AbsenceRequest(lesson, requester, request.reason());
        AbsenceRequest saved = absenceRequestRepository.save(absenceRequest);

        log.debug("결석 요청 생성 완료 (id={})", saved.getId());
        return AbsenceRequestResponse.from(saved);
    }

    public List<AbsenceRequestResponse> getAbsenceRequests(Long requesterId, boolean isAdmin, RequestStatus status) {
        log.debug("결석 요청 목록 조회 (isAdmin={}, status={})", isAdmin, status);

        List<AbsenceRequest> list;
        if (status != null) {
            list = isAdmin
                ? absenceRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                : absenceRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                    .stream()
                    .filter(r -> r.getRequestedBy().getId().equals(requesterId))
                    .toList();
        } else {
            list = isAdmin
                ? absenceRequestRepository.findAllByOrderByCreatedAtDesc()
                : absenceRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId);
        }

        return list.stream().map(AbsenceRequestResponse::from).toList();
    }

    public AbsenceRequestResponse getAbsenceRequest(Long requesterId, Long requestId, boolean isAdmin) {
        log.debug("결석 요청 상세 조회 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !absenceRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public AbsenceRequestResponse approveAbsenceRequest(Long approverId, Long requestId) {
        log.debug("결석 요청 승인 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new UserNotFoundException(approverId));

        absenceRequest.approve(approver);

        // 수업 출석 상태를 공결(EXCUSED)로 업데이트
        Lesson lesson = absenceRequest.getLesson();
        lesson.updateTeacherAttendance(TeacherAttendanceStatus.EXCUSED);

        log.debug("결석 요청 승인 완료 (requestId={})", requestId);
        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public AbsenceRequestResponse rejectAbsenceRequest(Long approverId, Long requestId, String note) {
        log.debug("결석 요청 반려 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new UserNotFoundException(approverId));

        absenceRequest.reject(approver, note);

        log.debug("결석 요청 반려 완료 (requestId={})", requestId);
        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public void deleteAbsenceRequest(Long requesterId, Long requestId, boolean isAdmin) {
        log.debug("결석 요청 삭제 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !absenceRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        absenceRequestRepository.delete(absenceRequest);
        log.debug("결석 요청 삭제 완료 (requestId={})", requestId);
    }
}
