package geumjeongyahak.domain.post.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "게시글 상세 응답 DTO입니다.")
public record PostDetailResponse(
        @Schema(description = "게시글 내부 식별자입니다.", example = "1")
        Long id,

        @Schema(description = "이 게시글이 속한 채널 ID입니다.", example = "2")
        Long channelId,

        @Schema(description = "게시글이 속한 채널의 표시 이름입니다.", example = "공지사항")
        String channelName,

        @Schema(description = "채널 유형입니다.", example = "NOTICE")
        String channelType,

        @Schema(description = "게시글 제목입니다.", example = "4월 운영 공지")
        String title,

        @Schema(description = "게시글 본문 HTML입니다.", example = "<p>공지 내용입니다.</p>")
        String contentHtml,

        @Schema(description = "게시글 상태입니다.", example = "PUBLISHED")
        String status,

        @Schema(description = "작성자 사용자 ID입니다.", example = "1")
        Long authorId,

        @Schema(description = "작성자 표시 이름입니다.", example = "Administrator")
        String authorName,

        @Schema(description = "상단 고정 여부입니다.", example = "false")
        boolean isPinned,

        @Schema(description = "댓글 허용 여부입니다.", example = "true")
        boolean allowComment,

        @Schema(description = "대표 썸네일 URL입니다. 없으면 null입니다.", nullable = true, example = "https://cdn.example.com/posts/thumbnail.png")
        String thumbnailUrl,

        @Schema(description = "초안 만료 시각입니다. PUBLISHED/ARCHIVED 상태이면 null입니다.", nullable = true, example = "2026-05-04T15:00:00")
        LocalDateTime expiresAt,

        @Schema(description = "누적 조회수입니다.", example = "146")
        long viewCount,

        @Schema(description = "게시글 생성 시각입니다.", example = "2026-04-10T19:30:00")
        LocalDateTime createdAt,

        @Schema(description = "마지막 수정 시각입니다.", example = "2026-04-10T19:30:00")
        LocalDateTime updatedAt,

        @Schema(description = "첨부파일 목록입니다. sortOrder 오름차순으로 정렬됩니다.")
        List<PostAttachmentInfo> attachments
) {
    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getChannel().getId(),
                post.getChannel().getName(),
                post.getChannel().getChannelType().name(),
                post.getTitle(),
                post.getContentHtml(),
                post.getStatus().name(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.isPinned(),
                post.isAllowComment(),
                post.getThumbnailUrl(),
                post.getExpiresAt(),
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getPostAttachments().stream()
                        .filter(pa -> !pa.getFile().isDeleted())
                        .map(pa -> new PostAttachmentInfo(
                                pa.getFile().getId(),
                                pa.getFile().getOriginalName(),
                                pa.getFile().getContentType(),
                                pa.getFile().getFileSize(),
                                pa.getFile().getExt(),
                                pa.getFile().isGoogleDrive(),
                                pa.getFile().getPublicUrl(),
                                pa.getSortOrder()))
                        .toList()
        );
    }

    @Schema(description = "첨부파일 정보입니다.")
    public record PostAttachmentInfo(
            @Schema(description = "파일 UUID입니다.", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID fileId,

            @Schema(description = "원본 파일명입니다.", example = "공지문.pdf")
            String originalName,

            @Schema(description = "MIME 타입입니다.", example = "application/pdf")
            String contentType,

            @Schema(description = "파일 크기(byte)입니다.", example = "204800")
            Long fileSize,

            @Schema(description = "파일 확장자입니다.", example = "pdf")
            String ext,

            @Schema(description = "Google Drive에 저장된 외부 파일이면 true입니다.", example = "false")
            boolean isGoogleDrive,

            @Schema(description = "다운로드 가능한 URL입니다.", example = "https://storage.googleapis.com/...")
            String downloadUrl,

            @Schema(description = "첨부파일 정렬 순서입니다.", example = "0")
            int sortOrder
    ) {}
}
