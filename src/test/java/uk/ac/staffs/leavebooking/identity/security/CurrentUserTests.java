package uk.ac.staffs.leavebooking.identity.security;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The signed-in user")
class CurrentUserTests {
    private final CurrentUser currentUser = new CurrentUser();

    @Test
    @DisplayName("The signed-in user has the expected identity, staff record and role")
    void retrievesFirebaseUidRoleAndStaffIdentity() {
        JwtAuthenticationToken authentication = authentication(Role.MANAGER, "manager-1");

        assertEquals("firebase-uid", currentUser.firebaseUid(authentication).orElseThrow());
        assertEquals("manager-1", currentUser.staffId(authentication).orElseThrow());
        assertTrue(currentUser.hasRole(authentication, Role.MANAGER));
        assertTrue(currentUser.isOwner(authentication, "manager-1"));
    }

    @Test
    @DisplayName("A user without a staff identity does not own a staff record")
    void missingStaffIdentityNeverImpliesOwnership() {
        JwtAuthenticationToken authentication = authentication(Role.ADMIN, null);

        assertTrue(currentUser.staffId(authentication).isEmpty());
        assertFalse(currentUser.isOwner(authentication, "staff-1"));
    }

    @Test
    @DisplayName("A user does not own another staff member's record")
    void aDifferentIdentityIsNotTheOwner() {
        assertFalse(currentUser.isOwner(authentication(Role.STAFF, "staff-1"), "staff-2"));
    }

    private JwtAuthenticationToken authentication(Role role, String staffId) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("firebase-uid")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("role", role.getAuthority());
        if (staffId != null) {
            builder.claim("staffId", staffId);
        }
        return new JwtAuthenticationToken(
                builder.build(),
                List.of(new SimpleGrantedAuthority(role.getAuthority()))
        );
    }
}
