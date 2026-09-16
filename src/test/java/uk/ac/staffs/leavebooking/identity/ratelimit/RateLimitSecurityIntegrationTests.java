package uk.ac.staffs.leavebooking.identity.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uk.ac.staffs.leavebooking.identity.authservice.FirebaseAuthService;
import uk.ac.staffs.leavebooking.identity.dto.LoginResponse;
import uk.ac.staffs.leavebooking.identity.security.Role;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "rabbitmq.enabled=false",
        "firebase.enabled=false",
        "rate-limit.enabled=true",
        "rate-limit.login.capacity=2",
        "rate-limit.login.window-seconds=60",
        "rate-limit.api.capacity=2",
        "rate-limit.api.window-seconds=60"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Request limits through the API")
class RateLimitSecurityIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private RateLimitInterceptor rateLimitInterceptor;
    @MockitoBean private FirebaseAuthService firebaseAuthService;

    @BeforeEach
    void clearBuckets() {
        rateLimitInterceptor.clearBuckets();
    }

    @Test
    @DisplayName("Anonymous login is limited per remote IP with the shared 429 contract")
    void loginIsLimitedByRemoteIp(CapturedOutput output) throws Exception {
        when(firebaseAuthService.loginUser(anyString(), anyString())).thenReturn(loginResponse());

        mockMvc.perform(loginFrom("192.0.2.10")).andExpect(status().isOk());
        mockMvc.perform(loginFrom("192.0.2.10")).andExpect(status().isOk());
        mockMvc.perform(loginFrom("192.0.2.10"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value(RateLimitInterceptor.RATE_LIMIT_EXCEEDED))
                .andExpect(jsonPath("$.message").value(RateLimitInterceptor.RATE_LIMIT_MESSAGE))
                .andExpect(jsonPath("$.path").value("/auth/login"));

        mockMvc.perform(loginFrom("192.0.2.11")).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(output)
                .contains("RATE_LIMIT outcome=429 method=POST endpoint=/auth/login")
                .contains("remoteIp=192.0.2.10");
    }

    @Test
    @DisplayName("Missing authentication remains 401 before authenticated API limiting")
    void anonymousProtectedRequestRemainsUnauthorised() throws Exception {
        mockMvc.perform(get("/api/staff/staff-1/leave-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("An authenticated but unauthorised request remains 403")
    void forbiddenRequestRemainsForbidden() throws Exception {
        mockMvc.perform(get("/api/staff/staff-2/leave-requests")
                        .with(as(Role.STAFF, "uid-staff-1", "staff-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Authenticated endpoint traffic is limited by Staff identity and matched pattern")
    void authenticatedEndpointIsLimited() throws Exception {
        RequestPostProcessor staff = as(Role.STAFF, "uid-staff-1", "staff-1");

        mockMvc.perform(get("/api/staff/staff-1/leave-requests").with(staff))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/staff-1/leave-requests").with(staff))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/staff-1/leave-requests").with(staff))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("Different resource IDs share the matched endpoint-pattern bucket")
    void differentResourceIdsShareEndpointPattern() throws Exception {
        RequestPostProcessor admin = as(Role.ADMIN, "uid-admin", null);

        mockMvc.perform(get("/api/staff/staff-a/leave-requests").with(admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/staff-b/leave-requests").with(admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/staff-c/leave-requests").with(admin))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Admin users without Staff identity are isolated by Firebase UID")
    void firebaseUidProvidesIndependentAdminBuckets() throws Exception {
        RequestPostProcessor firstAdmin = as(Role.ADMIN, "uid-admin-1", null);
        RequestPostProcessor secondAdmin = as(Role.ADMIN, "uid-admin-2", null);

        mockMvc.perform(get("/api/leave-requests").queryParam("status", "PENDING")
                        .with(firstAdmin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/leave-requests").queryParam("status", "PENDING")
                        .with(firstAdmin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/leave-requests").queryParam("status", "PENDING")
                        .with(firstAdmin)).andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/leave-requests").queryParam("status", "PENDING")
                        .with(secondAdmin)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Different matched endpoint patterns have independent authenticated limits")
    void endpointPatternsAreIndependent() throws Exception {
        RequestPostProcessor admin = as(Role.ADMIN, "uid-admin", null);
        mockMvc.perform(get("/api/staff/staff-1/leave-requests").with(admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/staff-1/leave-requests").with(admin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff/staff-1/leave-allowances").with(admin))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginFrom(
            String remoteAddress
    ) {
        return post("/auth/login")
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"admin@leavebooking.test","password":"test-password"}
                        """);
    }

    private RequestPostProcessor as(Role role, String uid, String staffId) {
        return jwt().jwt(token -> {
                    token.subject(uid).claim("role", role.getAuthority());
                    if (staffId != null) {
                        token.claim("staffId", staffId);
                    }
                })
                .authorities(new SimpleGrantedAuthority(role.getAuthority()));
    }

    private LoginResponse loginResponse() {
        return new LoginResponse(
                "uid-admin", "admin@leavebooking.test", "Admin",
                "id-token", "refresh-token", "3600"
        );
    }
}
