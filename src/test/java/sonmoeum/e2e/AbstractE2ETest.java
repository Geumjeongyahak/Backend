package sonmoeum.e2e;

import java.util.List;
import java.util.Map;

import sonmoeum.domain.auth.entity.UserPermission;
import sonmoeum.domain.auth.enums.PermissionGranterType;
import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.auth.repository.DepartmentPermissionRepository;
import sonmoeum.domain.auth.repository.PermissionRepository;
import sonmoeum.domain.auth.repository.UserPermissionRepository;
import sonmoeum.domain.classroom.repository.ClassroomRepository;
import sonmoeum.domain.department.repository.DepartmentRepository;
import sonmoeum.domain.department.repository.UserDepartmentRepository;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.request.repository.AbsenceRequestRepository;
import sonmoeum.domain.request.repository.LessonExchangeRequestRepository;
import sonmoeum.domain.request.repository.PurchaseRequestRepository;
import sonmoeum.domain.request.repository.SubjectExchangeRequestRepository;
import sonmoeum.domain.student.repository.StudentRepository;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractE2ETest {

    @LocalServerPort
    int port;

    // Domain repositories
    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected DepartmentRepository departmentRepository;

    @Autowired
    protected StudentRepository studentRepository;

    @Autowired
    protected ClassroomRepository classroomRepository;

    @Autowired
    protected SubjectRepository subjectRepository;

    @Autowired
    protected LessonRepository lessonRepository;

    // Auth repositories
    @Autowired
    protected PermissionRepository permissionRepository;

    @Autowired
    protected UserPermissionRepository userPermissionRepository;

    @Autowired
    protected DepartmentPermissionRepository departmentPermissionRepository;

    // Join table repositories
    @Autowired
    protected UserDepartmentRepository userDepartmentRepository;

    // Request repositories
    @Autowired
    protected AbsenceRequestRepository absenceRequestRepository;

    @Autowired
    protected LessonExchangeRequestRepository lessonExchangeRequestRepository;

    @Autowired
    protected SubjectExchangeRequestRepository subjectExchangeRequestRepository;

    @Autowired
    protected PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    public void tearDown() {
        // Delete in correct order to respect foreign key constraints

        // 1. Requests (highest dependency level)
        absenceRequestRepository.deleteAll();
        lessonExchangeRequestRepository.deleteAll();
        subjectExchangeRequestRepository.deleteAll();
        purchaseRequestRepository.deleteAll();

        // 2. Lessons
        lessonRepository.deleteAll();

        // 3. Subjects
        subjectRepository.deleteAll();

        // 4. Relationship tables
        userPermissionRepository.deleteAll();
        departmentPermissionRepository.deleteAll();
        userDepartmentRepository.deleteAll();

        // 5. Independent entities
        studentRepository.deleteAll();
        classroomRepository.deleteAll();
        departmentRepository.deleteAll();
        permissionRepository.deleteAll();

        // 6. Users (lowest dependency level)
        userRepository.deleteAll();
    }

    protected User createTestUser(String email, String password) {
        return userRepository.save(User.emailUserBuilder()
                .name("Test User")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(RoleType.VOLUNTEER)
                .phoneNumber("010-0000-0000")
                .permissions(List.of())
                .build());
    }

    /**
     * 관리자 사용자를 생성합니다.
     * SUPER_ADMIN 권한을 포함한 모든 권한을 부여합니다.
     */
    protected User createAdminUser(String email, String password) {
        User admin = userRepository.save(User.emailUserBuilder()
                .name("Admin User")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(RoleType.MANAGER)
                .phoneNumber("010-0000-0000")
                .permissions(List.of())
                .build());

        // SUPER_ADMIN 권한 부여
        grantPermission(admin, PermissionType.SUPER_ADMIN);

        return admin;
    }

    /**
     * 특정 권한을 가진 사용자를 생성합니다.
     */
    protected User createUserWithPermissions(String email, String password, PermissionType... permissions) {
        User user = userRepository.save(User.emailUserBuilder()
                .name("Test User")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(RoleType.VOLUNTEER)
                .phoneNumber("010-0000-0000")
                .permissions(List.of())
                .build());

        // 권한 부여
        for (PermissionType permission : permissions) {
            grantPermission(user, permission);
        }

        return user;
    }

    /**
     * 사용자에게 권한을 부여합니다.
     */
    protected void grantPermission(User user, PermissionType permissionType) {
        UserPermission userPermission = new UserPermission(
                user,
                permissionType,
                PermissionGranterType.USER
        );
        userPermissionRepository.save(userPermission);
    }

    /**
     * 로그인하고 세션 쿠키를 반환합니다.
     * REST Assured의 cookie() 메서드로 SESSION를 추출합니다.
     */
    protected String login(String email, String password) {
        io.restassured.response.Response response = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        // SESSION 쿠키 추출
        String sessionCookie = response.getCookie("SESSION");
        if (sessionCookie == null) {
            throw new IllegalStateException("로그인 후 SESSION 쿠키를 찾을 수 없습니다.");
        }
        return sessionCookie;
    }

    /**
     * 관리자로 로그인하고 세션 쿠키를 반환합니다.
     */
    protected String loginAsAdmin() {
        String email = "admin@example.com";
        String password = "admin123";
        createAdminUser(email, password);
        return login(email, password);
    }

    /**
     * 봉사자로 로그인하고 세션 쿠키를 반환합니다.
     */
    protected String loginAsVolunteer() {
        String email = "volunteer@example.com";
        String password = "volunteer123";
        createTestUser(email, password);
        return login(email, password);
    }

    /**
     * 세션 쿠키를 사용하는 인증된 RequestSpecification을 생성합니다.
     * 여러 요청에서 같은 세션을 유지할 때 사용합니다.
     */
    protected io.restassured.specification.RequestSpecification authenticatedRequest(String sessionCookie) {
        return given()
            .cookie("SESSION", sessionCookie);
    }
}
