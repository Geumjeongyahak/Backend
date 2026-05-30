package geumjeongyahak.domain.users.v1.controller;

import geumjeongyahak.domain.users.service.TeacherService;
import geumjeongyahak.domain.users.v1.dto.response.TeacherContactResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teachers")
@Tag(
    name = "Teacher",
    description = "교사 활동 정보와 연락망 조회 API입니다."
)
public class TeacherController {

    private static final String TEACHER_OR_HIGHER_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";

    private final TeacherService teacherService;

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "교사 연락망 조회",
        description = """
            현재 활동 중인 교사 목록을 연락망 화면용으로 조회합니다.

            조회 기준:
            - teacherStartAt이 오늘 이전 또는 오늘인 사용자
            - teacherEndAt이 없거나 오늘 이후 또는 오늘인 사용자

            응답 정보:
            - 교사 이름
            - 소속 반 이름
            - 연락처

            개인정보가 포함되므로 교사 이상의 권한을 가진 사용자만 조회할 수 있습니다.
            """
    )
    @GetMapping("/contact-list")
    public ResponseEntity<List<TeacherContactResponse>> getTeacherContacts() {
        log.debug("GET /api/v1/teachers/contact-list - 교사 연락망 조회 요청");
        return ResponseEntity.ok(teacherService.getCurrentTeacherContacts());
    }
}
