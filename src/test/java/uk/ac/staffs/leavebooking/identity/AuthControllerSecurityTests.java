package uk.ac.staffs.leavebooking.identity;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.staffs.leavebooking.GlobalExceptionHandler;
import uk.ac.staffs.leavebooking.identity.authservice.FirebaseAuthService;
import uk.ac.staffs.leavebooking.identity.dto.LoginResponse;
import uk.ac.staffs.leavebooking.identity.dto.RegisterRequest;
import uk.ac.staffs.leavebooking.identity.dto.RegisterResponse;
import uk.ac.staffs.leavebooking.identity.exceptions.AuthenticationFailedException;
import uk.ac.staffs.leavebooking.identity.security.ControllerSecurityTestConfiguration;
import uk.ac.staffs.leavebooking.identity.security.Role;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, ControllerSecurityTestConfiguration.class})
@DisplayName("Login and account access")
class AuthControllerSecurityTests {
    private static final String LOGIN_JSON = """
            {
              "email": "admin@leavebooking.test",
              "password": "local-password"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirebaseAuthService firebaseAuthService;

    @Test
    @DisplayName("Login is public but account registration requires authentication")
    void loginIsTheOnlyAnonymousIdentityOperation() throws Exception {
        when(firebaseAuthService.loginUser("admin@leavebooking.test", "local-password"))
                .thenReturn(new LoginResponse(
                        "uid-admin",
                        "admin@leavebooking.test",
                        "Admin",
                        "id-token",
                        "refresh-token",
                        "3600"
                ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localId").value("uid-admin"))
                .andExpect(jsonPath("$.idToken").value("id-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminRegistrationJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("Incorrect login details return 401 without exposing private information")
    void invalidCredentialsReturnSafeUnauthorisedResponse() throws Exception {
        when(firebaseAuthService.loginUser(any(), any()))
                .thenThrow(new AuthenticationFailedException("Authentication failed"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("Checking a role without signing in returns 401")
    void anonymousProtectedRequestReturnsShared401Contract() throws Exception {
        mockMvc.perform(get("/auth/role-check"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/auth/role-check"));
    }

    @Test
    @WithMockFirebaseUser(role = Role.STAFF)
    @DisplayName("Staff cannot create user accounts")
    void staffCannotRegisterFirebaseUsers() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminRegistrationJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockFirebaseUser(role = Role.ADMIN, staffId = "")
    @DisplayName("An admin can create an account and view their role")
    void adminCanRegisterAndCheckRole() throws Exception {
        when(firebaseAuthService.registerUser(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(
                        "uid-new", "new-admin@example.com", "New Admin", "User created successfully"
                ));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminRegistrationJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid").value("uid-new"));

        mockMvc.perform(get("/auth/role-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"));
    }

    private String adminRegistrationJson() {
        return """
                {
                  "username": "New Admin",
                  "email": "new-admin@example.com",
                  "password": "password",
                  "role": "ADMIN"
                }
                """;
    }
}
