package sonmoeum.e2e.departments;

import java.util.Map;

import sonmoeum.e2e.AbstractE2ETest;

public abstract class BaseDepartmentE2ETest extends AbstractE2ETest {

    protected Map<String, Object> createDepartmentRequest(String name, String description) {
        return Map.of(
            "name", name,
            "description", description
        );
    }

    protected String getAdminSession() {
        return loginAsAdmin();
    }
}
