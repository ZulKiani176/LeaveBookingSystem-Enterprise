package uk.ac.staffs.leavebooking.identity.authservice;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import uk.ac.staffs.leavebooking.identity.dto.LoginResponse;
import uk.ac.staffs.leavebooking.identity.dto.RegisterRequest;
import uk.ac.staffs.leavebooking.identity.dto.RegisterResponse;
import uk.ac.staffs.leavebooking.identity.exceptions.FirebaseConfigurationException;
import uk.ac.staffs.leavebooking.identity.exceptions.IdentityEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.identity.exceptions.IdentityRegistrationException;
import uk.ac.staffs.leavebooking.identity.security.Role;
import uk.ac.staffs.leavebooking.staff.ContextFacade;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FirebaseAuthService {
    public static final String USER_CREATED_CONFIRMATION = "User created successfully";
    public static final String AUTHENTICATION_FAILED = "Authentication failed";
    public static final String FIREBASE_DISABLED =
            "Firebase is disabled; set FIREBASE_ENABLED=true for identity operations";
    public static final String WEB_API_KEY_REQUIRED =
            "FIREBASE_WEB_API_KEY is required when Firebase is enabled";
    public static final String STAFF_ID_REQUIRED =
            "A staff member identity is required for this role";
    public static final String STAFF_EMAIL_MISMATCH =
            "Firebase email must match the linked staff member email";

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseAuthService.class);
    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final ContextFacade staffContextFacade;
    private final FirebaseProperties properties;
    private final FirebaseIdentityToolkitClient identityToolkitClient;

    public FirebaseAuthService(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            ContextFacade staffContextFacade,
            FirebaseProperties properties,
            FirebaseIdentityToolkitClient identityToolkitClient
    ) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.staffContextFacade = staffContextFacade;
        this.properties = properties;
        this.identityToolkitClient = identityToolkitClient;
    }

    public LoginResponse loginUser(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String normalisedEmail = email == null ? null : email.trim();
        LOG.info("Login attempt for {}", normalisedEmail);
        return identityToolkitClient.login(normalisedEmail, password);
    }

    public RegisterResponse registerUser(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        if (request.role() == null) {
            throw new IdentityRegistrationException("Role is required");
        }
        requireEnabled();
        FirebaseAuth firebaseAuth = requiredFirebaseAuth();
        String email = request.email().trim();
        String staffId = validateStaffLink(request.role(), request.staffMemberId(), email);

        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(request.password())
                .setDisplayName(request.username().trim())
                .setEmailVerified(false);

        UserRecord created = null;
        try {
            created = firebaseAuth.createUser(createRequest);
            firebaseAuth.setCustomUserClaims(
                    created.getUid(),
                    customClaims(request.role(), staffId)
            );
            return new RegisterResponse(
                    created.getUid(),
                    created.getEmail(),
                    created.getDisplayName(),
                    USER_CREATED_CONFIRMATION
            );
        } catch (FirebaseAuthException exception) {
            if (created != null) {
                deleteIncompleteUser(firebaseAuth, created.getUid());
            }
            if (exception.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new IdentityEmailAlreadyExistsException(email);
            }
            LOG.error("Firebase registration failed for {} with code {}",
                    email, exception.getAuthErrorCode());
            throw new IdentityRegistrationException("Firebase user registration failed", exception);
        }
    }

    private String validateStaffLink(Role role, String suppliedStaffId, String email) {
        String staffId = suppliedStaffId == null ? "" : suppliedStaffId.trim();
        if (role != Role.ADMIN && staffId.isBlank()) {
            throw new IdentityRegistrationException(STAFF_ID_REQUIRED);
        }
        if (staffId.isBlank()) {
            return null;
        }

        StaffMemberDTO staffMember = staffContextFacade.findStaffMemberById(staffId);
        if (!staffMember.email().equalsIgnoreCase(email)) {
            throw new IdentityRegistrationException(STAFF_EMAIL_MISMATCH);
        }
        return staffId;
    }

    private Map<String, Object> customClaims(Role role, String staffId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("role", role.getAuthority());
        if (staffId != null) {
            claims.put("staffId", staffId);
        }
        return Map.copyOf(claims);
    }

    private FirebaseAuth requiredFirebaseAuth() {
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            throw new FirebaseConfigurationException(FIREBASE_DISABLED);
        }
        return firebaseAuth;
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new FirebaseConfigurationException(FIREBASE_DISABLED);
        }
    }

    private void deleteIncompleteUser(FirebaseAuth firebaseAuth, String uid) {
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException cleanupFailure) {
            LOG.error("Unable to remove incomplete Firebase user {}", uid, cleanupFailure);
        }
    }
}
