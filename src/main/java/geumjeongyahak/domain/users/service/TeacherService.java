package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.v1.dto.response.TeacherContactResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private static final Set<RoleType> TEACHER_ROLES = Set.of(
        RoleType.VOLUNTEER,
        RoleType.MANAGER,
        RoleType.ADMIN
    );

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TeacherContactResponse> getCurrentTeacherContacts() {
        log.debug("대표 분반이 지정된 교사 연락망 조회 요청");

        List<User> teachers = userRepository
            .findAllByRoleInAndClassroomIsNotNullAndIsDeletedFalseOrderByNameAscIdAsc(TEACHER_ROLES);

        List<TeacherContactResponse> contacts = teachers.stream()
            .map(TeacherContactResponse::from)
            .toList();

        log.debug("대표 분반이 지정된 교사 연락망 조회 완료 - count: {}", contacts.size());
        return contacts;
    }
}
