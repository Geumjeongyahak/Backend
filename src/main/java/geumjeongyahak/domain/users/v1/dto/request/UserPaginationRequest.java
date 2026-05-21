package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;

@Schema(description = "사용자 목록 페이징 요청 DTO. page와 size를 이용해 사용자 목록 조회 범위를 지정합니다.")
@Getter
@Setter
public class UserPaginationRequest extends BasePaginationRequest {
    @Schema(description = "검색할 역할(Role)", example = "VOLUNTEER", nullable = true)
    private String role;

    @Schema(description = "검색할 닉네임(부분 일치)", example = "홍길동", nullable = true)
    private String nickname;

    @Schema(description = "검색할 이름(부분 일치)", example = "홍길동", nullable = true)
    private String name;

    @Schema(
        description = "true이면 teacherStartAt, teacherEndAt 기준으로 현재 활동 중인 선생님만 조회합니다.",
        example = "true",
        nullable = true
    )
    private Boolean currentTeacher;

    @Override
    public PageRequest toRequest() {
       return PageRequest.of(getPage(), getSize());
    }

    public boolean isCurrentTeacherOnly() {
        return Boolean.TRUE.equals(currentTeacher);
    }
}
