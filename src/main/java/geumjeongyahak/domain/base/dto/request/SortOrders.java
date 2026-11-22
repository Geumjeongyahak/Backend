package geumjeongyahak.domain.base.dto.request;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Sort;

/**
 * 목록 요청의 {@code sort} 파라미터("필드,방향;필드,방향")를 {@link Sort.Order} 목록으로 바꾼다.
 * 필드 허용 목록은 요청 DTO의 {@code @ValidSortField}가 먼저 본다. 여기서는 형식이 틀리면 400으로 끝낸다
 * (검증이 빠진 엔드포인트에서 500이 나지 않게).
 */
public final class SortOrders {

    private SortOrders() {
    }

    public static List<Sort.Order> parse(String sortFields) {
        if (sortFields == null || sortFields.isBlank()) {
            return List.of();
        }
        return Arrays.stream(sortFields.split(";"))
            .map(String::trim)
            .filter(sort -> !sort.isEmpty())
            .map(SortOrders::toOrder)
            .toList();
    }

    private static Sort.Order toOrder(String sort) {
        String[] parts = sort.split(",");
        if (parts.length != 2 || parts[0].isBlank()) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "정렬 조건은 '필드명,방향' 형식이어야 합니다: " + sort);
        }
        String field = parts[0].trim();
        return switch (parts[1].trim().toUpperCase()) {
            case "ASC" -> Sort.Order.asc(field);
            case "DESC" -> Sort.Order.desc(field);
            default -> throw new BadRequestException(
                CommonErrorCode.INVALID_INPUT, "정렬 방향은 ASC 또는 DESC만 사용할 수 있습니다: " + sort
            );
        };
    }
}
