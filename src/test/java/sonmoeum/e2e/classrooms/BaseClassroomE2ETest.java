package sonmoeum.e2e.classrooms;

import java.util.Map;

import sonmoeum.e2e.AbstractE2ETest;

public abstract class BaseClassroomE2ETest extends AbstractE2ETest {

    protected Map<String, Object> createClassroomRequest(String name, String type, String description) {
        return Map.of(
            "name", name,
            "type", type,
            "description", description
        );
    }

    protected String getAdminSession() {
        return loginAsAdmin();
    }
}
