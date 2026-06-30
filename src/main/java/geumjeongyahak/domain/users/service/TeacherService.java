package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.v1.dto.response.TeacherContactResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final UserRepository userRepository;
    private final SubjectProxyService subjectProxyService;

    @Transactional(readOnly = true)
    public List<TeacherContactResponse> getCurrentTeacherContacts() {
        LocalDate today = LocalDate.now();
        log.debug("현재 활동 교사 연락망 조회 요청 - today: {}", today);

        List<User> teachers = userRepository.findCurrentTeachersWithClassroom(today);
        Map<Long, String> assignedClassroomNames = getAssignedClassroomNames(teachers);

        List<TeacherContactResponse> contacts = teachers.stream()
            .map(teacher -> TeacherContactResponse.from(teacher, classroomNameOf(teacher, assignedClassroomNames)))
            .toList();

        log.debug("현재 활동 교사 연락망 조회 완료 - count: {}", contacts.size());
        return contacts;
    }

    private Map<Long, String> getAssignedClassroomNames(List<User> teachers) {
        List<Long> teacherIds = teachers.stream()
            .map(User::getId)
            .toList();

        return subjectProxyService.getActiveSubjectsByTeacherIds(teacherIds).stream()
            .collect(Collectors.toMap(
                subject -> subject.getTeacher().getId(),
                subject -> subject.getClassroom().getName(),
                (first, ignored) -> first
            ));
    }

    private String classroomNameOf(User teacher, Map<Long, String> assignedClassroomNames) {
        if (teacher.getClassroom() != null) {
            return teacher.getClassroom().getName();
        }
        return assignedClassroomNames.get(teacher.getId());
    }
}
