package sonmoeum.domain.classroom.service;

import sonmoeum.api.v1.classrooms.dto.request.CreateClassroomRequest;
import sonmoeum.api.v1.classrooms.dto.request.UpdateClassroomRequest;
import sonmoeum.api.v1.classrooms.dto.response.ClassroomResponse;
import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.repository.ClassroomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomService {
    private final ClassroomRepository classroomRepository;

    public ClassroomResponse getClassroomById(Long id) {
        Classroom classroom = classroomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 분반이 존재하지 않습니다."));
        return ClassroomResponse.from(classroom);
    }

    public BasePageResponse<ClassroomResponse> getClassroomPagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            classroomRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(ClassroomResponse::from);
    }

    @Transactional
    public ClassroomResponse createClassroom(CreateClassroomRequest request) {
        Classroom classroom = new Classroom(
            request.name(),
            request.type(),
            request.description()
        );
        return ClassroomResponse.from(classroomRepository.save(classroom));
    }

    @Transactional
    public ClassroomResponse updateClassroom(Long id, UpdateClassroomRequest request) {
        Classroom classroom = classroomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 분반이 존재하지 않습니다."));
        
        classroom.update(request.name(), request.type(), request.description());
        
        return ClassroomResponse.from(classroomRepository.save(classroom));
    }


    @Transactional
    public void deleteClassroom(Long id) {
        Classroom classroom = classroomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 분반이 존재하지 않습니다."));
        classroomRepository.delete(classroom);
    }
}
