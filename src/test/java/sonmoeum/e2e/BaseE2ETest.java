package sonmoeum.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.auth.v1.dto.request.LocalLoginRequest;
import sonmoeum.domain.auth.v1.dto.request.LocalSignupRequest;
import sonmoeum.domain.auth.v1.dto.response.LoginResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E 테스트 Base 클래스
 * - MockMvc를 사용한 통합 테스트
 * - 실제 HTTP 요청/응답 시뮬레이션
 * - JWT 토큰 기반 인증 지원
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("e2e")
@Transactional
public abstract class BaseE2ETest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // 테스트용 관리자 계정
    protected static final String ADMIN_USERNAME = "admin_e2e";
    protected static final String ADMIN_PASSWORD = "Admin123!@#";
    protected static final String ADMIN_NAME = "E2E Admin";
    protected static final String ADMIN_EMAIL = "admin_e2e@test.com";

    // 테스트용 일반 사용자 계정
    protected static final String USER_USERNAME = "user_e2e";
    protected static final String USER_PASSWORD = "User123!@#";
    protected static final String USER_NAME = "E2E User";
    protected static final String USER_EMAIL = "user_e2e@test.com";

    protected String adminAccessToken;
    protected String adminRefreshToken;
    protected String userAccessToken;
    protected String userRefreshToken;

    @BeforeEach
    protected void setUp() throws Exception {
        // 관리자 계정 생성 및 로그인
        setupAdminAccount();
        // 일반 사용자 계정 생성 및 로그인
        setupUserAccount();
    }

    /**
     * 관리자 계정 생성 및 로그인
     */
    protected void setupAdminAccount() throws Exception {
        // 회원가입
        LocalSignupRequest signupRequest = new LocalSignupRequest(
                ADMIN_USERNAME,
                ADMIN_PASSWORD,
                ADMIN_NAME,
                ADMIN_EMAIL,
                null
        );

        try {
            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isCreated());
        } catch (Exception e) {
            // 이미 존재하는 경우 무시
        }

        // 로그인
        LocalLoginRequest loginRequest = new LocalLoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );

        this.adminAccessToken = loginResponse.tokenResponse().accessToken();
        this.adminRefreshToken = loginResponse.tokenResponse().refreshToken();
    }

    /**
     * 일반 사용자 계정 생성 및 로그인
     */
    protected void setupUserAccount() throws Exception {
        // 회원가입
        LocalSignupRequest signupRequest = new LocalSignupRequest(
                USER_USERNAME,
                USER_PASSWORD,
                USER_NAME,
                USER_EMAIL,
                null
        );

        try {
            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isCreated());
        } catch (Exception e) {
            // 이미 존재하는 경우 무시
        }

        // 로그인
        LocalLoginRequest loginRequest = new LocalLoginRequest(USER_USERNAME, USER_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );

        this.userAccessToken = loginResponse.tokenResponse().accessToken();
        this.userRefreshToken = loginResponse.tokenResponse().refreshToken();
    }

    /**
     * Authorization 헤더 생성
     */
    protected String authHeader(String accessToken) {
        return "Bearer " + accessToken;
    }
}
