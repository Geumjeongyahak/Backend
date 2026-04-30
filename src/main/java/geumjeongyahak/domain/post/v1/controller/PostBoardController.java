package geumjeongyahak.domain.post.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.post.service.PostCrudService;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostSummaryResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Tag(
        name = "Post Board",
        description = """
                채널을 가로질러 게시글을 통합 조회하는 API입니다.
                전체글 보기, 공지사항 모아보기, 반별 게시판 탭, 부서별 게시판 탭 같은 화면을 구성할 때 사용합니다.
                개별 채널 내부 CRUD는 Post API가 담당하고, 이 API는 여러 채널을 한 목록으로 묶어 보여주는 역할에 집중합니다.
                """
)
public class PostBoardController {
    private final PostCrudService postCrudService;

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('post:read:*')")
    @Operation(
            summary = "통합 게시글 목록 조회",
            description = """
                    채널을 가로질러 게시글을 조회합니다.
                    전체 게시판, 공지사항 모아보기, 반별/부서별 게시판 탭을 구성할 때 사용할 수 있습니다.

                    예시:
                    - 전체 게시판: 필터 없이 호출
                    - 공지사항만: postType=NOTICE
                    - 반별 게시판: channelType=CLASSROOM
                    - 특정 반 게시판: classroomId={id}
                    - 부서별 게시판: channelType=DEPARTMENT
                    - 특정 부서 게시판: departmentId={id}

                    동작 방식:
                    - 채널 활성 상태와 삭제 여부를 반영해 노출 가능한 게시글만 조회합니다.
                    - 고정 글 우선, 최신 생성 글 순으로 정렬됩니다.
                    - channelId, channelType, classroomId, departmentId를 조합해 원하는 범위를 줄일 수 있습니다.

                    사이드 이펙트:
                    - 읽기 전용 API이며 게시글 조회수는 증가하지 않습니다.
                    - 게시판 통합 목록 구성용 응답으로, 각 글의 채널명/채널유형/작성자/조회수를 함께 제공합니다.
                    """
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<PostSummaryResponse>> getPosts(
            @ParameterObject @Valid PostSearchRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/posts - 통합 게시글 목록 조회 요청");
        return ResponseEntity.ok(postCrudService.getPosts(userDetails, request));
    }
}
