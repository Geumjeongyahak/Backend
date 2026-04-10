package sonmoeum.domain.post.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import sonmoeum.domain.base.dto.request.BasePaginationRequest;

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
                    게시글 유형 필터입니다.
                    NOTICE만 조회하면 공지사항 탭, GENERAL만 조회하면 일반 글 탭처럼 활용할 수 있습니다.
                    """,
            example = "NOTICE"
    )
    private String postType;

    @Schema(
            description = """
                    게시글 상태 필터입니다.
                    기본 운영 화면은 보통 PUBLISHED를 사용하고, 관리자 화면에서는 DRAFT 포함 조회 같은 용도로 확장할 수 있습니다.
                    """,
            example = "PUBLISHED"
    )
    private String status;

    @Schema(
            description = """
                    특정 채널 ID로 범위를 제한합니다.
                    통합 게시판 API에서 단일 채널만 별도로 조회하고 싶을 때 사용할 수 있습니다.
                    """,
            example = "2"
    )
    private Long channelId;

    @Schema(
            description = """
                    채널 유형 필터입니다.
                    CLASSROOM이면 반 게시판 전체, DEPARTMENT면 부서 게시판 전체, ALL이면 전역 공지 채널 중심 조회 같은 구성이 가능합니다.
                    """,
            example = "CLASSROOM"
    )
    private String channelType;

    @Schema(
            description = """
                    특정 분반 게시판만 조회할 때 사용하는 분반 ID입니다.
                    내부적으로 CLASSROOM 타입 채널 중 refId가 일치하는 채널의 게시글만 반환합니다.
                    """,
            example = "3"
    )
    private Long classroomId;

    @Schema(
            description = """
                    특정 부서 게시판만 조회할 때 사용하는 부서 ID입니다.
                    내부적으로 DEPARTMENT 타입 채널 중 refId가 일치하는 채널의 게시글만 반환합니다.
                    """,
            example = "2"
    )
    private Long departmentId;

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
