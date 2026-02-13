package sonmoeum.domain.subject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.exception.ClassroomNotFoundException;
import sonmoeum.domain.classroom.repository.ClassroomRepository;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.exception.SubjectDuplicateException;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.subject.v1.dto.request.CreateSubjectRequest;
import sonmoeum.domain.subject.v1.dto.response.SubjectDetailResponse;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubjectDetailResponse createSubject(CreateSubjectRequest request) {
        log.debug("과목 등록 요청 (name={})", request.name());
        Classroom classroom = classroomRepository.findById(request.classroomId())
            .orElseThrow(() -> {
                log.info("과목 등록 실패 - 교실을 찾을 수 없습니다. ID: {}", request.classroomId());
                return new ClassroomNotFoundException(request.classroomId());
            });
        User teacher = userRepository.findById(request.teacherId())
            .orElseThrow(() -> {
                log.info("과목 등록 실패 - 교사를 찾을 수 없습니다. ID: {}", request.teacherId());
                return new UserNotFoundException(request.teacherId());
            });

        // 같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재하는지 확인
        if (subjectRepository.existsByClassroomIdAndDayOfWeekAndPeriodAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            classroom.getId(),
            request.dayOfWeek(),
            request.period(),
            request.endAt(),
            request.startAt()
        )) {
            log.info("중복 검사 파라미터: classroomId={}, dayOfWeek={}, period={}, startAt={}, endAt={}",
                classroom.getId(), request.dayOfWeek(), request.period(), request.startAt(), request.endAt());
            log.info("과목 등록 실패 - 같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재합니다.");
            throw new SubjectDuplicateException("같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재합니다.");
        }

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
        log.debug("과목 등록 완료 (id={})", savedSubject.getId());
        return SubjectDetailResponse.from(savedSubject);
    }
}
