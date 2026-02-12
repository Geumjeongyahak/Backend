package sonmoeum.domain.classroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.enums.ClassroomType;
import sonmoeum.domain.classroom.exception.DuplicateClassroomNameException;
import sonmoeum.domain.classroom.repository.ClassroomRepository;
import sonmoeum.domain.classroom.v1.dto.request.CreateClassroomRequest;
import sonmoeum.domain.classroom.v1.dto.response.ClassroomResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomCrudService {
    private final ClassroomRepository classroomRepository;

    public ClassroomResponse createClassroom(CreateClassroomRequest request) {
        log.info("분반 생성 시도: {}", request.name());

        if (classroomRepository.existsByName(request.name())) {
            log.info("분반 생성 실패 - 중복된 이름: {}", request.name());
            throw new DuplicateClassroomNameException();
        }

        Classroom classroom =  classroomRepository.save(Classroom.builder()
                .name(request.name())
                .type(ClassroomType.valueOf(request.type()))
                .description(request.description())
                .build());

        log.info("분반 생성 성공: {}", classroom.getName());
        return ClassroomResponse.from(classroom);
    }
}
