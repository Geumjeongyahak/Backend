package geumjeongyahak.domain.classroom.service;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.DuplicateResourceException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.event.ClassroomCreatedEvent;
import geumjeongyahak.domain.classroom.event.ClassroomDeletedEvent;
import geumjeongyahak.domain.classroom.exception.ClassroomErrorCode;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.classroom.repository.specification.ClassroomSpecs;
import geumjeongyahak.domain.classroom.v1.dto.request.ClassroomPaginationRequest;
import geumjeongyahak.domain.classroom.v1.dto.request.CreateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.request.UpdateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomDetailResponse;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomSummaryResponse;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomCrudService {
    private final ClassroomRepository classroomRepository;
    private final EventPublisher eventPublisher;
    private final SubjectProxyService subjectProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public ClassroomDetailResponse createClassroom(CreateClassroomRequest request) {
        log.debug("분반 생성 시도: {}", request.name());

        if (classroomRepository.existsByName(request.name())) {
            // TODO: soft delete된 분반이 있을 경우 복구하는 로직 추가 고려(논의 필요)
            log.info("분반 생성 실패 - 중복된 이름: {}", request.name());
            throw new DuplicateResourceException(ClassroomErrorCode.DUPLICATE_CLASSROOM);
        }

        Classroom classroom = classroomRepository.save(Classroom.builder()
            .name(request.name())
            .type(ClassroomType.valueOf(request.type()))
            .description(request.description())
            .build());

        eventPublisher.publish(new ClassroomCreatedEvent(classroom.getId(), classroom.getName()));
        log.info("분반 생성 성공: {}", classroom.getName());
        return ClassroomDetailResponse.from(classroom);
    }

    public PaginationResponse<ClassroomSummaryResponse> getClassrooms(
        ClassroomPaginationRequest request
    ) {
        log.debug("분반 목록 조회 시도: name={}, type={}", request.getName(), request.getType());
        Specification<Classroom> spec = ClassroomSpecs.withoutDeleted();

        var pageRequest = request.toRequest();
        if (request.getName() != null) {
            spec = spec.and(ClassroomSpecs.containsName(request.getName()));
        }
        if (request.getType() != null) {
            spec = spec.and(ClassroomSpecs.hasType(ClassroomType.valueOf(request.getType())));
        }

        var pageResponse = PaginationResponse.from(
            classroomRepository.findAll(spec, pageRequest),
            ClassroomSummaryResponse::from
        );
        log.debug("분반 목록 조회 성공: {}개 조회", pageResponse.getTotalElements());
        return pageResponse;
    }

    public ClassroomDetailResponse getClassroomDetail(Long id) {
        log.debug("분반 상세 조회 시도: {}", id);
        Classroom classroom = getClassroomWithoutDeleted(id);
        log.debug("분반 상세 조회 성공: {}", classroom.getName());
        return ClassroomDetailResponse.from(classroom);
    }

    @Transactional
    public ClassroomDetailResponse updateClassroom(Long id, UpdateClassroomRequest request) {
        log.debug("분반 수정 시도: {}, {}, {}", request.name(), request.type(), request.description());

        Classroom classroom = getClassroomWithoutDeleted(id);
        String newName = request.name();
        ClassroomType newType = request.type() != null ? ClassroomType.valueOf(request.type()) : null;
        String newDescription = request.description();

        boolean nameChanged = newName != null && !newName.equals(classroom.getName());
        boolean typeChanged = newType != null && newType != classroom.getType();
        boolean descriptionChanged = newDescription != null
            && !Objects.equals(newDescription, classroom.getDescription());

        if (!nameChanged && !typeChanged && !descriptionChanged) {
            log.debug("분반 수정 요청에 변경된 값이 없어 기존 리소스를 반환합니다: {}", classroom.getName());
            return ClassroomDetailResponse.from(classroom);
        }

        if (nameChanged) {
            if (classroomRepository.existsByNameAndIdNot(newName, id)) {
                log.info("분반 수정 실패 - 중복된 이름: {}", newName);
                throw new DuplicateResourceException(ClassroomErrorCode.DUPLICATE_CLASSROOM);
            }
            classroom.setName(newName);
        }
        if (typeChanged) {
            classroom.setType(newType);
        }
        if (descriptionChanged) {
            classroom.setDescription(newDescription);
        }

        classroomRepository.save(classroom);
        log.info("분반 수정 성공: {}", classroom.getName());
        return ClassroomDetailResponse.from(classroom);
    }

    @Transactional
    public void deleteClassroom(Long id) {
        log.debug("분반 삭제 시도: {}", id);
        Classroom classroom = getClassroomWithoutDeleted(id);
        validateClassroomDeletable(id);
        classroom.setDeleted(true);
        classroomRepository.save(classroom);
        eventPublisher.publish(new ClassroomDeletedEvent(id));
        log.info("분반 삭제 성공: {}", classroom.getName());
    }

    private Classroom getClassroomWithoutDeleted(Long id) {
        Classroom classroom = classroomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ClassroomErrorCode.CLASSROOM_NOT_FOUND));
        if (classroom.isDeleted()) {
            throw new ResourceNotFoundException(ClassroomErrorCode.CLASSROOM_NOT_FOUND);
        }
        return classroom;
    }

    private void validateClassroomDeletable(Long classroomId) {
        if (subjectProxyService.existsActiveSubjectByClassroomId(classroomId)) {
            throw new BusinessException(
                CommonErrorCode.INVALID_STATE,
                "활성 과목이 연결된 분반은 삭제할 수 없습니다."
            );
        }
        if (userProxyService.existsByClassroomId(classroomId)) {
            throw new BusinessException(
                CommonErrorCode.INVALID_STATE,
                "기본 분반으로 사용 중인 사용자가 있는 분반은 삭제할 수 없습니다."
            );
        }
    }
}
