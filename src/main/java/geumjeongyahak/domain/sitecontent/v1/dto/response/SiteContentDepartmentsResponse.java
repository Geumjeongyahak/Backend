package geumjeongyahak.domain.sitecontent.v1.dto.response;

import java.util.List;

public record SiteContentDepartmentsResponse(
    SiteContentDepartmentResponse principal,
    List<SiteContentDepartmentResponse> departments
) {
}
