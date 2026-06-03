package geumjeongyahak.unit.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.event.ClassroomDeletedEvent;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.classroom.service.ClassroomCrudService;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.service.UserProxyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClassroomCrudServiceTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private SubjectProxyService subjectProxyService;

    @Mock
    private UserProxyService userProxyService;

    @InjectMocks
    private ClassroomCrudService classroomCrudService;

    @Test
    void deleteClassroom_marksClassroomDeletedAndPublishesDeletedEvent() {
        Classroom classroom = Classroom.builder()
            .name("삭제 대상 분반")
            .type(ClassroomType.WEEKDAY)
            .description("삭제 이벤트 테스트")
            .build();
        ReflectionTestUtils.setField(classroom, "id", 10L);

        given(classroomRepository.findById(10L)).willReturn(Optional.of(classroom));
        given(subjectProxyService.existsActiveSubjectByClassroomId(10L)).willReturn(false);
        given(userProxyService.existsByClassroomId(10L)).willReturn(false);

        classroomCrudService.deleteClassroom(10L);

        assertThat(classroom.isDeleted()).isTrue();
        verify(classroomRepository).save(classroom);

        ArgumentCaptor<ClassroomDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ClassroomDeletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().classroomId()).isEqualTo(10L);
    }
}
