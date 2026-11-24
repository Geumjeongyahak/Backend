package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeroomAssignmentService {

    private final UserRepository userRepository;
    private final UserProxyService userProxyService;
    private final ClassroomProxyService classroomProxyService;

    @Transactional
    public User assign(Long userId, Long classroomId) {
        log.debug("담임 업무분장 지정 요청 - userId: {}, classroomId: {}", userId, classroomId);

        Classroom classroom = classroomProxyService.getActiveById(classroomId);
        User targetUser = userProxyService.getById(userId);

        userRepository.findByHomeroomClassroomIdAndIsDeletedFalse(classroomId)
            .filter(currentHomeroom -> !currentHomeroom.getId().equals(userId))
            .ifPresent(currentHomeroom -> {
                currentHomeroom.releaseHomeroomClassroom();
                userRepository.flush();
            });

        targetUser.assignHomeroomClassroom(classroom);
        log.info("담임 업무분장 지정 완료 - userId: {}, classroomId: {}", userId, classroomId);
        return targetUser;
    }

    @Transactional
    public void release(Long userId) {
        log.debug("담임 업무분장 해제 요청 - userId: {}", userId);
        User user = userProxyService.getById(userId);
        user.releaseHomeroomClassroom();
        log.info("담임 업무분장 해제 완료 - userId: {}", userId);
    }
}
