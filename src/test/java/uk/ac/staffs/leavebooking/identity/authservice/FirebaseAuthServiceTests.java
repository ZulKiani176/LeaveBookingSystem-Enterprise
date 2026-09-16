package uk.ac.staffs.leavebooking.identity.authservice;

import org.junit.jupiter.api.DisplayName;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import uk.ac.staffs.leavebooking.identity.dto.LoginResponse;
import uk.ac.staffs.leavebooking.identity.dto.RegisterRequest;
import uk.ac.staffs.leavebooking.identity.exceptions.IdentityEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.identity.exceptions.IdentityRegistrationException;
import uk.ac.staffs.leavebooking.identity.security.Role;
import uk.ac.staffs.leavebooking.staff.ContextFacade;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@DisplayName("Firebase login and account creation")
class FirebaseAuthServiceTests {
    private ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private FirebaseAuth firebaseAuth;
    private ContextFacade staffContextFacade;
    private FirebaseIdentityToolkitClient identityToolkitClient;
    private FirebaseAuthService service;

    @BeforeEach
    void setUp() {
        firebaseAuthProvider = mock(ObjectProvider.class);
        firebaseAuth = mock(FirebaseAuth.class);
        staffContextFacade = mock(ContextFacade.class);
        identityToolkitClient = mock(FirebaseIdentityToolkitClient.class);
        when(firebaseAuthProvider.getIfAvailable()).thenReturn(firebaseAuth);
        service = new FirebaseAuthService(
                firebaseAuthProvider,
                staffContextFacade,
                new FirebaseProperties(true, FirebaseProperties.EXPECTED_PROJECT_ID, "key", "path"),
                identityToolkitClient
        );
    }

    @Test
    @DisplayName("Login ignores spaces around the email and returns the Firebase result")
    void loginTrimsEmailAndReturnsFirebaseResponse() {
        LoginResponse expected = new LoginResponse(
                "uid-1", "ada@example.com", "Ada", "id-token", "refresh", "3600"
        );
        when(identityToolkitClient.login("ada@example.com", "password"))
                .thenReturn(expected);

        LoginResponse actual = service.loginUser(" ada@example.com ", "password");

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("A staff account receives the staff role and linked staff identity")
    void staffRegistrationValidatesLinkAndSetsMinimalClaims() throws Exception {
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staffMember());
        UserRecord user = firebaseUser();
        when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(user);

        var response = service.registerUser(request(Role.STAFF, "staff-1"));

        assertEquals("uid-1", response.uid());
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(firebaseAuth).setCustomUserClaims(org.mockito.ArgumentMatchers.eq("uid-1"), claims.capture());
        assertEquals(Map.of("role", "ROLE_STAFF", "staffId", "staff-1"), claims.getValue());
    }

    @Test
    @DisplayName("An admin account does not need a linked staff record")
    void adminRegistrationDoesNotRequireOrCreateStaffClaim() throws Exception {
        UserRecord user = firebaseUser();
        when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(user);

        service.registerUser(request(Role.ADMIN, null));

        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(firebaseAuth).setCustomUserClaims(org.mockito.ArgumentMatchers.eq("uid-1"), claims.capture());
        assertEquals(Map.of("role", "ROLE_ADMIN"), claims.getValue());
        verify(staffContextFacade, never()).findStaffMemberById(any());
    }

    @Test
    @DisplayName("A manager account cannot be created without a staff identity")
    void nonAdminRoleRequiresStaffIdentity() throws Exception {
        assertThrows(
                IdentityRegistrationException.class,
                () -> service.registerUser(request(Role.MANAGER, "  "))
        );
        verify(firebaseAuth, never()).createUser(any());
    }

    @Test
    @DisplayName("An account email must match its linked staff record")
    void linkedStaffEmailMustMatchFirebaseEmail() throws Exception {
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staffMember());

        RegisterRequest request = new RegisterRequest(
                "Grace", "grace@example.com", "password", Role.HR, "staff-1"
        );

        assertThrows(IdentityRegistrationException.class, () -> service.registerUser(request));
        verify(firebaseAuth, never()).createUser(any());
    }

    @Test
    @DisplayName("An email already registered in Firebase is rejected")
    void duplicateFirebaseEmailMapsToStableApplicationException() throws Exception {
        when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class)))
                .thenThrow(new FirebaseAuthException(
                        ErrorCode.ALREADY_EXISTS,
                        "duplicate",
                        null,
                        null,
                        AuthErrorCode.EMAIL_ALREADY_EXISTS
                ));

        assertThrows(
                IdentityEmailAlreadyExistsException.class,
                () -> service.registerUser(request(Role.ADMIN, null))
        );
    }

    private RegisterRequest request(Role role, String staffId) {
        return new RegisterRequest(
                "Ada", "ada@example.com", "password", role, staffId
        );
    }

    private UserRecord firebaseUser() {
        UserRecord user = mock(UserRecord.class);
        when(user.getUid()).thenReturn("uid-1");
        when(user.getEmail()).thenReturn("ada@example.com");
        when(user.getDisplayName()).thenReturn("Ada");
        return user;
    }

    private StaffMemberDTO staffMember() {
        return new StaffMemberDTO(
                "staff-1",
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(2025, 1, 1),
                "Engineering",
                "manager-1",
                "Developer",
                LocalDate.of(2025, 1, 1),
                "Senior",
                "Permanent",
                EmploymentStatus.ACTIVE
        );
    }
}
