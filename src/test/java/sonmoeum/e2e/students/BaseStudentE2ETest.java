package sonmoeum.e2e.students;

import java.util.Map;

import sonmoeum.e2e.AbstractE2ETest;

public abstract class BaseStudentE2ETest extends AbstractE2ETest {

    protected Map<String, Object> createStudentRequest(String name, String grade, String schoolName) {
        return Map.of(
            "name", name,
            "grade", grade,
            "schoolName", schoolName,
            "parentPhoneNumber", "010-9876-5432"
        );
    }

    protected String getAdminSession() {
        return loginAsAdmin();
    }
}
