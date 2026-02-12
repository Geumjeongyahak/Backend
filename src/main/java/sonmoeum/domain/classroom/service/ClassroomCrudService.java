package sonmoeum.domain.classroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.DuplicateResourceException;
import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.enums.ClassroomType;
import sonmoeum.domain.classroom.repository.ClassroomRepository;
import sonmoeum.domain.classroom.v1.dto.request.CreateClassroomRequest;
import sonmoeum.domain.classroom.v1.dto.request.UpdateClassroomRequest;
import sonmoeum.domain.classroom.v1.dto.response.ClassroomResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomCrudService {
    private final ClassroomRepository classroomRepository;

    public ClassroomResponse createClassroom(CreateClassroomRequest request) {
        log.debug("분반 생성 시도: {}", request.name());

        if (classroomRepository.existsByName(request.name())) {
            log.info("분반 생성 실패 - 중복된 이름: {}", request.name());
            throw new DuplicateResourceException(ErrorCode.DUPLICATE_CLASSROOM);
        }

        Classroom classroom =  classroomRepository.save(Classroom.builder()
                .name(request.name())
                .type(ClassroomType.valueOf(request.type()))
                .description(request.description())
                .build());

        log.info("분반 생성 성공: {}", classroom.getName());
        return ClassroomResponse.from(classroom);
    }

    public ClassroomResponse updateClassroom(Long id, UpdateClassroomRequest request) {
        log.debug("분반 수정 시도: {}, {}, {}", request.name(), request.type(), request.description());

        boolean isUpdated = false;
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CLASSROOM_NOT_FOUND));

        if (request.name() != null) {
            if (classroomRepository.existsByName(request.name())) {
                log.info("분반 수정 실패 - 중복된 이름: {}", request.name());
                throw new DuplicateResourceException(ErrorCode.DUPLICATE_CLASSROOM);
            }
            classroom.setName(request.name());
            isUpdated = true;
        }
        if (request.type() != null) {
            classroom.setType(ClassroomType.valueOf(request.type()));
            isUpdated = true;
        }
        if (request.description() != null) {
            classroom.setDescription(request.description());
            isUpdated = true;
        }
        if (!isUpdated) {
            log.info("분반 수정 실패 - 변경된 값이 없음: {}", classroom.getName());
            throw new BusinessException(ErrorCode.NO_CHANGES_DETECTED);
        }
        classroomRepository.save(classroom);
        log.info("분반 수정 성공: {}", classroom.getName());
        return ClassroomResponse.from(classroom);
    }
}
