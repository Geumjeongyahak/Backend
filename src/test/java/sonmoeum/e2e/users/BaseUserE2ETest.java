package sonmoeum.e2e.users;

import java.util.Map;

import sonmoeum.e2e.AbstractE2ETest;

public abstract class BaseUserE2ETest extends AbstractE2ETest {

    protected Map<String, Object> createUserRequest(String email, String password, String name) {
        return Map.of(
            "email", email,
            "password", password,
            "name", name,
            "phoneNumber", "010-1234-5678",
            "role", "VOLUNTEER"
        );
    }

    protected String getAdminSession() {
        return loginAsAdmin();
    }
}
