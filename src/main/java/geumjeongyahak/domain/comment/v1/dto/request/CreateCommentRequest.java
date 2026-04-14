package geumjeongyahak.domain.comment.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        description = """
                댓글 생성 요청 DTO입니다.
                게시글에 일반 댓글을 달거나, 기존 댓글에 답글을 달 때 사용합니다.
                parentCommentId를 비우면 일반 댓글, 값을 넣으면 같은 게시글 내부 대댓글로 처리됩니다.
                """
)
public record CreateCommentRequest(
        @Schema(
                description = """
                        댓글 본문입니다.
                        한 줄 의견부터 간단한 안내, 질문, 답변까지 자유롭게 입력할 수 있습니다.
                        """,
                example = "확인했습니다. 다음 주 일정도 같이 공지 부탁드립니다."
        )
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 2000, message = "댓글 내용은 2000자 이하로 입력해주세요.")
        String content,

        @Schema(
                description = """
                        부모 댓글 ID입니다.
                        비워두면 일반 댓글이고, 값을 넣으면 해당 댓글의 답글로 생성됩니다.
                        다른 게시글에 속한 댓글 ID는 사용할 수 없습니다.
                        """,
                example = "12",
                nullable = true
        )
        Long parentCommentId
) {
}
