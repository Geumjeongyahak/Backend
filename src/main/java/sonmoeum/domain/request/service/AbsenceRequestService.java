package sonmoeum.domain.request.service;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.requests.dto.request.CreateAbsenceRequest;
import sonmoeum.api.v1.requests.dto.request.RequestStatusUpdateRequest;
import sonmoeum.api.v1.requests.dto.response.AbsenceRequestResponse;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.request.entity.AbsenceRequest;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.repository.AbsenceRequestRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AbsenceRequestService {
    private final AbsenceRequestRepository absenceRequestRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;


    public BasePageResponse<AbsenceRequestResponse> getAbsenceRequestPagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            absenceRequestRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(AbsenceRequestResponse::from);
    }

    public AbsenceRequestResponse getAbsenceRequestById(Long id) {
        AbsenceRequest request = absenceRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 요청이 존재하지 않습니다."));
        return AbsenceRequestResponse.from(request);
    }

    @Transactional
    public AbsenceRequestResponse createAbsenceRequest(Long userId, CreateAbsenceRequest request) {
        Lesson lesson = lessonRepository.findById(request.lessonId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 수업이 존재하지 않습니다."));
        User requestedBy = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다."));

        AbsenceRequest absenceRequest = new AbsenceRequest(
            lesson,
            requestedBy,
            request.reason()
        );
        return AbsenceRequestResponse.from(absenceRequestRepository.save(absenceRequest));
    }


    @Transactional
    public AbsenceRequestResponse updateStatus(Long id, Long approverId, RequestStatusUpdateRequest request) {
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 요청이 존재하지 않습니다."));
        
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 승인자가 존재하지 않습니다."));
        
        if (request.status() == RequestStatus.APPROVED) {
            absenceRequest.approve(approver);
        } else if (request.status() == RequestStatus.REJECTED) {
            absenceRequest.reject(approver, request.note());
        } else {
             throw new IllegalArgumentException("유효하지 않은 상태 변경 요청입니다.");
        }


        return AbsenceRequestResponse.from(absenceRequestRepository.save(absenceRequest));
    }
}
