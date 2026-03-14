package sonmoeum.domain.subject.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.exception.ClassroomNotFoundException;
import sonmoeum.domain.classroom.repository.ClassroomRepository;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.exception.InvalidSubjectScheduleException;
import sonmoeum.domain.subject.exception.SubjectDuplicateException;
import sonmoeum.domain.subject.exception.SubjectNotFoundException;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.subject.v1.dto.request.CreateSubjectRequest;
import sonmoeum.domain.subject.v1.dto.request.UpdateSubjectRequest;
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

    public SubjectDetailResponse getSubject(Long subjectId) {
        log.debug("과목 단건 조회 요청 (id={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 단건 조회 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });

        log.debug("과목 단건 조회 완료 (id={})", subject.getId());
        return SubjectDetailResponse.from(subject);
    }

    public List<SubjectDetailResponse> getAllSubjects(Long classroomId) {
        log.debug("과목 목록 조회 요청 (classroomId={})", classroomId);

        if (classroomId != null) {
            classroomRepository.findById(classroomId)
                .orElseThrow(() -> {
                    log.info("과목 목록 조회 실패 - 분반을 찾을 수 없습니다. ID: {}", classroomId);
                    return new ClassroomNotFoundException(classroomId);
                });

            return subjectRepository.findByClassroomId(classroomId).stream()
                .map(SubjectDetailResponse::from)
                .toList();
        }

        return subjectRepository.findAll().stream()
            .map(SubjectDetailResponse::from)
            .toList();
    }

    @Transactional
    public SubjectDetailResponse updateSubject(Long subjectId, UpdateSubjectRequest request) {
        log.debug("과목 수정 요청 (id={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 수정 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });

        // 연관 엔티티는 요청에 들어왔을 때만 교체
        Classroom classroom = subject.getClassroom();
        if (request.classroomId() != null) {
            classroom = classroomRepository.findById(request.classroomId())
                .orElseThrow(() -> {
                    log.info("과목 수정 실패 - 분반을 찾을 수 없습니다. ID: {}", request.classroomId());
                    return new ClassroomNotFoundException(request.classroomId());
                });
        }

        User teacher = subject.getTeacher();
        if (request.teacherId() != null) {
            teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> {
                    log.info("과목 PATCH 수정 실패 - 선생님을 찾을 수 없습니다. ID: {}", request.teacherId());
                    return new UserNotFoundException(request.teacherId());
                });
        }

        // 요청 값이 null이면 기존 값 유지
        String name = request.name() != null ? request.name() : subject.getName();
        LocalDate startAt = request.startAt() != null ? request.startAt() : subject.getStartAt();
        LocalDate endAt = request.endAt() != null ? request.endAt() : subject.getEndAt();
        Integer times = request.times() != null ? request.times() : subject.getTimes();
        DayOfWeek dayOfWeek = request.dayOfWeek() != null ? request.dayOfWeek() : subject.getDayOfWeek();
        LocalTime startTime = request.startTime() != null ? request.startTime() : subject.getStartTime();
        LocalTime endTime = request.endTime() != null ? request.endTime() : subject.getEndTime();
        Integer period = request.period() != null ? request.period() : subject.getPeriod();
        String description = request.description() != null ? request.description() : subject.getDescription();

        // 값 검증
        validateScheduleRange(startAt, endAt, startTime, endTime);
        validatePatchBasics(name, times, period); // 필요 시

        // 비즈니스 검증: 중복 스케줄(자기 자신 제외)
        // 같은 분반에서 이 과목을 제외하고 기간이 겹치는 과목 중 요일과 교시가 일치하는 다른 과목이 존재하는지 확인
        boolean conflict = subjectRepository.existsByIdNotAndClassroomIdAndDayOfWeekAndPeriodAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            subjectId,
            classroom.getId(),
            dayOfWeek,
            period,
            endAt,
            startAt
        );

        if (conflict) {
            log.info("과목 수정 실패 - 같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재합니다.");
            throw new SubjectDuplicateException("같은 분반에서 기간이 겹치는 과목 중 요일과 교시가 일치하는 과목이 존재합니다.");
        }

        // 실제 반영
        subject.update(
            classroom,
            teacher,
            name,
            startAt,
            endAt,
            times,
            dayOfWeek,
            startTime,
            endTime,
            period,
            description
        );

        Subject saved = subjectRepository.save(subject);
        log.debug("과목 PATCH 수정 완료 (id={})", saved.getId());
        return SubjectDetailResponse.from(saved);
    }

    @Transactional
    public void deleteSubject(Long subjectId) {
        log.debug("과목 삭제(비활성화) 요청 (id={})", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> {
                log.info("과목 삭제 실패 - 과목을 찾을 수 없습니다. ID: {}", subjectId);
                return new SubjectNotFoundException(subjectId);
            });

        // 이미 비활성화면 멱등하게 처리(성공 처리)
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            log.debug("과목은 이미 비활성화 상태입니다. (id={})", subjectId);
            return;
        }

        subject.deactivate();
        subjectRepository.save(subject);

        log.debug("과목 삭제(비활성화) 완료 (id={})", subjectId);
    }

    private void validateScheduleRange(LocalDate startAt, LocalDate endAt, LocalTime startTime, LocalTime endTime) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new InvalidSubjectScheduleException("startAt은 endAt보다 늦을 수 없습니다.");
        }
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new InvalidSubjectScheduleException("startTime은 endTime보다 빨라야 합니다.");
        }
    }

    private void validatePatchBasics(String name, Integer times, Integer period) {
        if (name != null && name.isBlank()) {
            throw new InvalidSubjectScheduleException("name은 공백일 수 없습니다.");
        }
        if (times != null && times < 1) {
            throw new InvalidSubjectScheduleException("times는 1 이상이어야 합니다.");
        }
        if (period != null && period < 1) {
            throw new InvalidSubjectScheduleException("period는 1 이상이어야 합니다.");
        }
    }
}
