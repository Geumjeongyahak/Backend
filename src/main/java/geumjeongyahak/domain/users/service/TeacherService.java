package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.v1.dto.response.TeacherContactResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TeacherContactResponse> getCurrentTeacherContacts() {
        LocalDate today = LocalDate.now();
        log.debug("현재 활동 교사 연락망 조회 요청 - today: {}", today);

        List<TeacherContactResponse> contacts = userRepository.findCurrentTeachersWithClassroom(today).stream()
            .map(TeacherContactResponse::from)
            .toList();

        log.debug("현재 활동 교사 연락망 조회 완료 - count: {}", contacts.size());
        return contacts;
    }
}
