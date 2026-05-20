package geumjeongyahak.domain.post.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostBoardSearchRequest extends PostSearchRequest {

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
}
