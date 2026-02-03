package sonmoeum.domain.subject.service;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.subjects.dto.request.CreateSubjectRequest;
import sonmoeum.api.v1.subjects.dto.request.UpdateSubjectRequest;
import sonmoeum.api.v1.subjects.dto.response.SubjectResponse;
import sonmoeum.common.event.EventPublisher;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.repository.ClassroomRepository;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.event.SubjectCreatedEvent;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;


    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 과목이 존재하지 않습니다."));
        return SubjectResponse.from(subject);
    }

    public BasePageResponse<SubjectResponse> getSubjectPagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            subjectRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(SubjectResponse::from);
    }

    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        Classroom classroom = classroomRepository.findById(request.classId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 분반이 존재하지 않습니다."));
        User teacher = userRepository.findById(request.teacherId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 강사가 존재하지 않습니다."));

        Subject subject = new Subject(
            classroom,
            teacher,
            request.name(),
            request.startAt(),
            request.endAt(),
            request.times(),
            request.dayOfWeek(),
            request.startTime(),
            request.endTime(),
            request.period(),
            request.description()
        );

        Subject savedSubject = subjectRepository.save(subject);

        eventPublisher.publish(new SubjectCreatedEvent(
            savedSubject.getId(),
            savedSubject.getTeacher().getId(),
            savedSubject.getStartAt(),
            savedSubject.getEndAt(),
            savedSubject.getTimes(),
            savedSubject.getDayOfWeek(),
            savedSubject.getStartTime(),
            savedSubject.getEndTime()
        ));


        return SubjectResponse.from(savedSubject);
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, UpdateSubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 과목이 존재하지 않습니다."));
        
        User teacher = userRepository.findById(request.teacherId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 강사가 존재하지 않습니다."));

        subject.update(request.name(), teacher, request.description());
        
        return SubjectResponse.from(subjectRepository.save(subject));
    }


    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 과목이 존재하지 않습니다."));
        subjectRepository.delete(subject);
    }
}
