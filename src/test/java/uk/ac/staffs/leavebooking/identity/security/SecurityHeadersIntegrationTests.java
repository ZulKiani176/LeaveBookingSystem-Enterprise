package uk.ac.staffs.leavebooking.identity.security;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "rabbitmq.enabled=false",
        "firebase.enabled=false",
        "rate-limit.enabled=false"
})
@AutoConfigureMockMvc
@DisplayName("Security headers")
class SecurityHeadersIntegrationTests {
    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'none'; frame-ancestors 'none'";

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Successful responses include the required security headers")
    void successfulResponsesContainRestrictiveSecurityHeaders() throws Exception {
        ResultActions response = mockMvc.perform(get("/api/staff/staff-1/leave-requests")
                        .with(asStaff("staff-1")))
                .andExpect(status().isOk());

        assertSecurityHeaders(response);
    }

    @Test
    @DisplayName("Responses to unsigned-in users include the required security headers")
    void unauthorisedResponsesContainRestrictiveSecurityHeaders() throws Exception {
        ResultActions response = mockMvc.perform(get("/api/staff/staff-1/leave-requests"))
                .andExpect(status().isUnauthorized());

        assertSecurityHeaders(response);
    }

    @Test
    @DisplayName("Access-denied responses include the required security headers")
    void forbiddenResponsesContainRestrictiveSecurityHeaders() throws Exception {
        ResultActions response = mockMvc.perform(get("/api/staff/staff-2/leave-requests")
                        .with(asStaff("staff-1")))
                .andExpect(status().isForbidden());

        assertSecurityHeaders(response);
    }

    private void assertSecurityHeaders(ResultActions response) throws Exception {
        response.andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", CONTENT_SECURITY_POLICY))
                .andExpect(header().doesNotExist("X-Powered-By"));
    }

    private RequestPostProcessor asStaff(String staffId) {
        return jwt().jwt(token -> token
                        .subject("firebase-staff")
                        .claim("role", Role.STAFF.getAuthority())
                        .claim("staffId", staffId))
                .authorities(new SimpleGrantedAuthority(Role.STAFF.getAuthority()));
    }
}
