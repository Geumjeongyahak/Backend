package geumjeongyahak.unit.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class BasePaginationRequestSortTest {

    /** toSortOrders는 protected라 하위 클래스로 부른다. */
    private static final class SortProbe extends BasePaginationRequest {
        List<Sort.Order> parse(String sort) {
            return toSortOrders(sort);
        }

        @Override
        public PageRequest toRequest() {
            return PageRequest.of(0, 10);
        }
    }

    private final SortProbe probe = new SortProbe();

    @Test
    void emptyOrNullMeansNoOrder() {
        assertThat(probe.parse(null)).isEmpty();
        assertThat(probe.parse("")).isEmpty();
        assertThat(probe.parse(";")).isEmpty();
    }

    @Test
    void missingDirectionIsBadRequestNot500() {
        assertThatThrownBy(() -> probe.parse("createdAt")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void directionIsCaseInsensitiveAndTrimmed() {
        assertThat(probe.parse(" name , desc ; id,Asc")).containsExactly(Sort.Order.desc("name"), Sort.Order.asc("id"));
    }

    @Test
    void unknownDirectionIsBadRequest() {
        assertThatThrownBy(() -> probe.parse("createdAt,up")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void blankFieldIsBadRequest() {
        assertThatThrownBy(() -> probe.parse(",ASC")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void extraPartIsBadRequest() {
        assertThatThrownBy(() -> probe.parse("createdAt,ASC,more")).isInstanceOf(BadRequestException.class);
    }
}
