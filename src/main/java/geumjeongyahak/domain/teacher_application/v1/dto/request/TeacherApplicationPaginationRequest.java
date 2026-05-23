package geumjeongyahak.domain.teacher_application.v1.dto.request;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 교원 신청 목록 페이지네이션 요청")
public class TeacherApplicationPaginationRequest extends BasePaginationRequest {

    @Schema(description = "신청자 이름, 이메일, 연락처, 선호 과목명 검색어", example = "홍길동")
    private String keyword;

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(
            getPage(),
            getSize(),
            Sort.by(List.of(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            ))
        );
    }
}
