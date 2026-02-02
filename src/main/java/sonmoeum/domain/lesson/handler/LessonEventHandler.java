package sonmoeum.domain.lesson.handler;

import sonmoeum.domain.lesson.service.LessonService;
import sonmoeum.domain.subject.event.SubjectCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LessonEventHandler {
    private final LessonService lessonService;

    @Async
    @EventListener
    @Transactional
    public void handleSubjectCreated(SubjectCreatedEvent event) {
        lessonService.createLessonsFromSubject(event);
    }
}
