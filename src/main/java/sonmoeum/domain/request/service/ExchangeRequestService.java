package sonmoeum.domain.request.service;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.requests.dto.request.CreateExchangeRequest;
import sonmoeum.api.v1.requests.dto.request.RequestStatusUpdateRequest;
import sonmoeum.api.v1.requests.dto.response.ExchangeRequestResponse;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.request.entity.LessonExchangeRequest;
import sonmoeum.domain.request.entity.SubjectExchangeRequest;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.repository.LessonExchangeRequestRepository;
import sonmoeum.domain.request.repository.SubjectExchangeRequestRepository;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExchangeRequestService {
    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;
    private final SubjectExchangeRequestRepository subjectExchangeRequestRepository;
    private final LessonRepository lessonRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;


    public BasePageResponse<ExchangeRequestResponse> getLessonExchangePagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            lessonExchangeRequestRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(ExchangeRequestResponse::from);
    }

    public BasePageResponse<ExchangeRequestResponse> getSubjectExchangePagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            subjectExchangeRequestRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(ExchangeRequestResponse::from);
    }

    @Transactional
    public ExchangeRequestResponse createLessonExchange(Long userId, CreateExchangeRequest request) {
        Lesson lesson = lessonRepository.findById(request.targetId())
        .orElseThrow(() -> new IllegalArgumentException("해당 ID의 수업이 존재하지 않습니다."));
        User requestedBy = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다."));

        LessonExchangeRequest exchangeRequest = new LessonExchangeRequest(
            lesson,
            requestedBy,
            request.title(),
            request.content()
        );
        return ExchangeRequestResponse.from(lessonExchangeRequestRepository.save(exchangeRequest));
    }


    @Transactional
    public ExchangeRequestResponse createSubjectExchange(Long userId, CreateExchangeRequest request) {
        Subject subject = subjectRepository.findById(request.targetId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 과목이 존재하지 않습니다."));
        User requestedBy = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다."));

        SubjectExchangeRequest exchangeRequest = new SubjectExchangeRequest(
            subject,
            requestedBy,
            request.title(),
            request.content()
        );
        return ExchangeRequestResponse.from(subjectExchangeRequestRepository.save(exchangeRequest));
    }


    @Transactional
    public ExchangeRequestResponse updateLessonExchangeStatus(Long id, Long approverId, RequestStatusUpdateRequest request) {
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 요청이 존재하지 않습니다."));
        
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 승인자가 존재하지 않습니다."));
        
        if (request.status() == RequestStatus.APPROVED) {
            exchangeRequest.approve(approver);
        } else if (request.status() == RequestStatus.REJECTED) {
            exchangeRequest.reject(approver, request.note());
        }
        return ExchangeRequestResponse.from(lessonExchangeRequestRepository.save(exchangeRequest));

    }

    @Transactional
    public ExchangeRequestResponse updateSubjectExchangeStatus(Long id, Long approverId, RequestStatusUpdateRequest request) {
         SubjectExchangeRequest exchangeRequest = subjectExchangeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 요청이 존재하지 않습니다."));
        
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 승인자가 존재하지 않습니다."));
        
        if (request.status() == RequestStatus.APPROVED) {
            exchangeRequest.approve(approver);
        } else if (request.status() == RequestStatus.REJECTED) {
            exchangeRequest.reject(approver, request.note());
        }
        return ExchangeRequestResponse.from(subjectExchangeRequestRepository.save(exchangeRequest));

    }
}
