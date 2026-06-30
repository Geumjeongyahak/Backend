package geumjeongyahak.domain.post.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;

import java.util.List;

@Getter
@Setter
public class PostSearchRequest extends BasePaginationRequest {

    @Schema(
            description = """
                    게시자 이름 또는 아이디 기준 부분 검색 조건입니다.
                    채널 내부 목록 조회와 통합 게시판 조회 모두에서 사용할 수 있습니다.
                    """,
            example = "홍길동"
    )
    private String author;

    @Schema(
            description = """
                    게시글 제목 부분 검색 조건입니다.
                    예를 들어 '공지'를 넣으면 제목에 '공지'가 포함된 글을 찾습니다.
                    """,
            example = "공지"
    )
    private String title;

    @Schema(
            description = """
                    게시글 본문 HTML 내부 텍스트 기준 검색 조건입니다.
                    운영 키워드나 특정 안내 문구가 들어간 글을 찾을 때 사용합니다.
                    """,
            example = "운영 일정"
    )
    private String content;

    @Schema(
            description = """
                    게시글 상태 필터입니다.
                    기본 운영 화면은 보통 PUBLISHED를 사용하고, 관리자 화면에서는 DRAFT 포함 조회 같은 용도로 확장할 수 있습니다.
                    """,
            example = "PUBLISHED"
    )
    @ValidPostStatus
    private String status;

    @Schema(
            description = """
                    상단 고정 글 여부 필터입니다.
                    true면 공지처럼 강조된 글만, false면 일반 위치 글만 따로 볼 수 있습니다.
                    """,
            example = "true"
    )
    private Boolean isPinned;

    public PostSearchRequest() {
        super();
    }

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(
                this.getPage(),
                this.getSize(),
                Sort.by(List.of(
                        Sort.Order.desc("isPinned"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                ))
        );
    }
}
