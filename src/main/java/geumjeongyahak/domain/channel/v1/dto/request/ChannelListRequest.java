package geumjeongyahak.domain.channel.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.validation.annotation.ValidChannelBindingType;
import geumjeongyahak.common.validation.annotation.ValidChannelType;
import geumjeongyahak.common.validation.annotation.ValidSortField;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChannelListRequest {

    @Schema(
            description = """
                    채널 이름 부분 검색 조건입니다.
                    예를 들어 '공지'를 입력하면 '공지사항', '운영 공지' 같은 채널을 함께 찾을 수 있습니다.
                    관리자 운영 화면에서 특정 채널을 빠르게 찾는 용도로 자주 사용합니다.
                    """,
            example = "공지"
    )
    private String name;

    @Schema(
            description = """
                    채널 유형 필터입니다.
                    NOTICE, EVENT, RESOURCE, CLASSROOM, DEPARTMENT, GUIDE, CUSTOM 중 하나를 입력하면 해당 유형만 조회합니다.
                    예를 들어 반 게시판만 보고 싶으면 CLASSROOM, 부서 게시판만 보고 싶으면 DEPARTMENT를 사용합니다.
                    """,
            example = "CLASSROOM"
    )
    @ValidChannelType
    private String channelType;

    @Schema(
            description = """
                    채널 연동 구분 필터입니다.
                    STANDALONE 또는 DOMAIN_LINKED 중 하나를 입력하면 해당 범위만 조회합니다.
                    공지/이벤트/자료실/커스텀 채널만 보려면 STANDALONE, 분반/부서 연동 채널만 보려면 DOMAIN_LINKED를 사용합니다.
                    """,
            example = "DOMAIN_LINKED"
    )
    @ValidChannelBindingType
    private String bindingType;

    @Schema(
            description = "활성 상태 필터입니다. true이면 현재 사용 가능한 채널만, false이면 숨김 상태 채널만 조회합니다. 운영 중단 채널 점검 시 유용합니다.",
            example = "true"
    )
    private Boolean isActive;

    @Schema(
            description = "기본 채널 여부 필터입니다. 운영상 기본 채널만 따로 보고 싶을 때 사용합니다. 초기 게시판 메뉴 구성 검토 시 활용할 수 있습니다.",
            example = "false"
    )
    private Boolean isDefault;

    @Schema(
            description = """
                    특정 분반에 연결된 채널만 조회할 때 사용하는 분반 ID입니다.
                    내부적으로 CLASSROOM 타입 채널 중 refId가 일치하는 채널만 반환합니다.
                    반별 게시판 설정이 실제로 만들어졌는지 확인할 때 유용합니다.
                    """,
            example = "3"
    )
    private Long classroomId;

    @Schema(
            description = """
                    특정 부서에 연결된 채널만 조회할 때 사용하는 부서 ID입니다.
                    내부적으로 DEPARTMENT 타입 채널 중 refId가 일치하는 채널만 반환합니다.
                    부서 전용 게시판 운영 현황 점검에 사용할 수 있습니다.
                    """,
            example = "2"
    )
    private Long departmentId;

    @Schema(
            description = """
                    정렬 조건입니다.
                    '필드명,방향' 형식을 세미콜론(;)으로 이어서 여러 개 지정할 수 있습니다.
                    허용 필드: id, name, lastPostedAt, createdAt, updatedAt

                    예:
                    - createdAt,DESC
                    - lastPostedAt,DESC;createdAt,DESC

                    값을 주지 않으면 기본값으로 createdAt 내림차순, id 내림차순이 적용됩니다.
                    관리자 화면에서는 최근 게시 채널 우선 확인을 위해 lastPostedAt,DESC 같은 정렬도 유용합니다.
                    """,
            example = "lastPostedAt,DESC;createdAt,DESC"
    )
    @ValidSortField(fields = {"id", "name", "lastPostedAt", "createdAt", "updatedAt"})
    private String sort;

    public Sort toSort() {
        List<Sort.Order> orders = toSortOrders(this.sort);
        if (orders.isEmpty()) {
            orders = List.of(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        }
        return Sort.by(orders);
    }

    private List<Sort.Order> toSortOrders(String sortFields) {
        if (sortFields == null || sortFields.isEmpty()) {
            return List.of();
        }
        List<Sort.Order> orders = new ArrayList<>();
        String[] sorts = sortFields.split(";");
        for (String sort : sorts) {
            if (sort.isEmpty()) {
                continue;
            }
            String[] parts = sort.split(",");
            if (parts.length != 2) {
                throw new BadRequestException(
                        CommonErrorCode.INVALID_INPUT,
                        "정렬 조건은 '필드명,방향' 형식이어야 합니다: " + sort
                );
            }
            String field = parts[0].trim();
            String direction = parts[1].trim().toUpperCase();
            if (direction.equals("ASC")) {
                orders.add(Sort.Order.asc(field));
            } else if (direction.equals("DESC")) {
                orders.add(Sort.Order.desc(field));
            } else {
                throw new BadRequestException(
                        CommonErrorCode.INVALID_INPUT,
                        "정렬 방향은 ASC 또는 DESC만 사용할 수 있습니다: " + sort
                );
            }
        }
        return orders;
    }
}
