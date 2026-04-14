package geumjeongyahak.domain.post.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.post.service.PostCrudService;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;
import geumjeongyahak.domain.post.v1.dto.request.UpdatePostRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import geumjeongyahak.domain.post.v1.dto.response.PostSummaryResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/posts")
@Tag(
        name = "Post",
        description = """
                특정 채널 하위에서 게시글을 생성, 조회, 수정, 삭제하는 API입니다.
                공지사항 채널, 반 게시판, 부서 게시판 같은 실제 게시판 화면의 글 단위를 다룰 때 사용합니다.
                채널 자체 설정은 Channel API가 담당하고, 이 API는 해당 채널 안의 게시글 데이터만 관리합니다.
                """
)
public class PostController {
    private final PostCrudService postCrudService;

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "게시글 생성",
            description = """
                    특정 채널 하위에 새 게시글을 생성합니다.

                    사용 사례:
                    - 공지사항 채널에 운영 공지 등록
                    - 특정 반 게시판에 수업 안내 글 작성
                    - 특정 부서 게시판에 회의 공지 게시

                    동작 방식:
                    - channelId는 경로로 전달되며, 요청 본문에는 포함하지 않습니다.
                    - 작성 권한은 채널의 writerPolicy에 따라 검증됩니다.
                    - 게시글 생성 직후 상세 응답이 반환됩니다.

                    사이드 이펙트:
                    - posts 테이블에 새 레코드가 생성됩니다.
                    - 해당 채널의 최근 게시 시각(lastPostedAt)이 함께 갱신됩니다.
                    - 기본 상태를 따로 주지 않으면 PUBLISHED로 처리됩니다.

                    예시:
                    - 공지 채널 운영 공지 작성
                    - 반 게시판 과제 안내 등록
                    - 부서 게시판 회의록 초안 작성
                    """
    )
    @PostMapping
    public ResponseEntity<PostDetailResponse> createPost(
            @PathVariable Long channelId,
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                postCrudService.createPost(channelId, userDetails.getUserId(), userDetails.isAdminOrManager(), request)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "게시글 목록 조회",
            description = """
                    특정 채널에 속한 게시글 목록을 페이지네이션하여 조회합니다.

                    사용 사례:
                    - 공지사항 게시판 목록 화면 구성
                    - 특정 반 게시판 내 제목/작성자 검색
                    - 부서 게시판에서 공지 글만 별도 조회

                    동작 방식:
                    - 검색 조건은 해당 채널 내부 게시글에만 적용됩니다.
                    - 고정 글이 먼저 정렬되고, 그다음 최신 생성 글 순으로 내려옵니다.
                    - 목록 응답에는 화면 구성에 필요한 채널 정보, 작성자, 조회수 요약이 포함됩니다.

                    사이드 이펙트:
                    - 읽기 전용 API입니다.
                    - 조회수는 증가하지 않습니다. 조회수 증가는 상세 조회 시점에만 반영됩니다.

                    예시:
                    - 제목에 '공지'가 포함된 글 조회
                    - NOTICE 유형 글만 조회
                    - 고정 글 여부로 필터링
                    """
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<PostSummaryResponse>> getPosts(
            @PathVariable Long channelId,
            @ParameterObject @Valid PostSearchRequest request
    ) {
        return ResponseEntity.ok(postCrudService.getPosts(channelId, request));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "게시글 상세 조회",
            description = """
                    특정 채널에 속한 게시글 상세 정보를 조회합니다.

                    사용 사례:
                    - 게시판 목록에서 글을 클릭했을 때 상세 내용 표시
                    - 수정 화면 진입 전 기존 게시글 데이터 조회
                    - 작성자, 상태, 댓글 허용 여부, 본문 HTML 확인

                    동작 방식:
                    - channelId와 postId가 모두 일치하는 게시글만 조회합니다.
                    - 삭제된 게시글이나 접근 불가 채널의 글은 찾을 수 없습니다.

                    사이드 이펙트:
                    - 조회 성공 시 해당 게시글의 조회수가 1 증가합니다.
                    - 읽기 요청이지만 게시글의 viewCount는 변경됩니다.

                    예시:
                    - 공지 상세 보기
                    - 반 게시판 수업 안내 본문 확인
                    - 부서 게시글 수정 전 원본 조회
                    """
    )
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long channelId,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(postCrudService.getPost(channelId, postId));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "게시글 수정",
            description = """
                    특정 채널에 속한 게시글을 수정합니다.

                    사용 사례:
                    - 공지 제목/본문 수정
                    - 일반 글을 공지 글로 승격
                    - 댓글 허용 여부나 상단 고정 여부 변경

                    동작 방식:
                    - 다른 채널로 이동하는 기능은 제공하지 않습니다.
                    - 작성자 본인 또는 관리자/매니저만 수정할 수 있습니다.
                    - 전달한 값만 반영되는 부분 수정 형태가 아니라, null이 아닌 필드만 덮어쓰는 갱신 방식입니다.
                    - 변경값이 하나도 없으면 오류가 반환됩니다.

                    사이드 이펙트:
                    - posts 테이블의 메타데이터나 본문이 즉시 갱신됩니다.
                    - 채널의 최근 게시 시각(lastPostedAt)이 다시 계산되어 갱신됩니다.

                    예시:
                    - 오타 수정
                    - 공지글 상단 고정 처리
                    - 상태를 DRAFT에서 PUBLISHED로 변경
                    """
    )
    @PutMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> updatePost(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                postCrudService.updatePost(
                        channelId,
                        userDetails.getUserId(),
                        userDetails.isAdminOrManager(),
                        postId,
                        request
                )
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "게시글 삭제",
            description = """
                    특정 채널에 속한 게시글을 소프트 삭제합니다.

                    사용 사례:
                    - 잘못 등록된 공지 제거
                    - 기간이 지난 반 게시판 안내글 정리
                    - 작성자가 자신의 일반 글을 삭제

                    동작 방식:
                    - 물리 삭제가 아니라 isDeleted=true로 처리됩니다.
                    - 작성자 본인 또는 관리자/매니저만 삭제할 수 있습니다.

                    사이드 이펙트:
                    - 이후 일반 목록/상세 조회에서 해당 게시글은 제외됩니다.
                    - 채널의 최근 게시 시각(lastPostedAt)이 남아 있는 최신 게시글 기준으로 다시 계산됩니다.

                    예시:
                    - 중복 공지 삭제
                    - 오등록 게시글 정리
                    """
    )
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long channelId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postCrudService.deletePost(channelId, userDetails.getUserId(), userDetails.isAdminOrManager(), postId);
        return ResponseEntity.noContent().build();
    }
}
