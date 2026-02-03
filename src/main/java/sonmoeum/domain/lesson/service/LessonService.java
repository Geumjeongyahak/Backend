package sonmoeum.domain.lesson.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.lessons.dto.response.LessonResponse;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.repository.LessonRepository;
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
public class LessonService {
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;


    public LessonResponse getLessonById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 수업이 존재하지 않습니다."));
        return LessonResponse.from(lesson);
    }

    public BasePageResponse<LessonResponse> getLessonPagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            lessonRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(LessonResponse::from);
    }

    public List<LessonResponse> getMyLessons(Long teacherId, LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusMonths(1);
        if (to == null) to = LocalDate.now().plusMonths(1);
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 강사가 존재하지 않습니다."));
        return lessonRepository.findAllByTeacherAndDateBetween(teacher, from, to).stream()
            .map(LessonResponse::from)
            .toList();
    }


    @Transactional
    public LessonResponse updateAttendance(Long lessonId, Lesson.AttendanceStatus status) {
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 수업이 존재하지 않습니다."));
        lesson.updateAttendance(status);
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public void createLessonsFromSubject(SubjectCreatedEvent event) {
        Subject subject = subjectRepository.findById(event.subjectId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 과목이 존재하지 않습니다."));
        User teacher = userRepository.findById(event.teacherId())
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 강사가 존재하지 않습니다."));

        List<Lesson> lessons = new ArrayList<>();
        LocalDate currentDate = event.startAt();
        int createdCount = 0;

        // 시작일이 해당 요일이 아니면 다음 해당 요일로 이동
        while (currentDate.getDayOfWeek() != event.dayOfWeek()) {
            currentDate = currentDate.plusDays(1);
        }

        while (createdCount < event.times() && !currentDate.isAfter(event.endAt())) {
            lessons.add(new Lesson(
                subject,
                teacher,
                currentDate,
                event.startTime(),
                event.endTime(),
                Lesson.AttendanceStatus.PENDING
            ));
            currentDate = currentDate.plusWeeks(1);
            createdCount++;
        }

        lessonRepository.saveAll(lessons);
    }

}
