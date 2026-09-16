package uk.ac.staffs.leavebooking.identity.security;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Roles in Firebase tokens")
class FirebaseJwtAuthenticationConverterTests {
    private final FirebaseJwtAuthenticationConverter converter =
            new FirebaseJwtAuthenticationConverter();

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(Role.class)
    @DisplayName("A valid Firebase role gives the user the matching authority")
    void validFirebaseRoleBecomesExactlyOneAuthority(Role role) {
        JwtAuthenticationToken authentication = (JwtAuthenticationToken) converter.convert(
                jwt(role.getAuthority(), "staff-1")
        );

        assertEquals("firebase-uid", authentication.getName());
        assertEquals(
                role.getAuthority(),
                authentication.getAuthorities().iterator().next().getAuthority()
        );
        assertEquals("staff-1", authentication.getToken().getClaimAsString("staffId"));
    }

    @Test
    @DisplayName("A token without a role cannot sign in")
    void missingRoleCannotAuthenticate() {
        assertThrows(
                OAuth2AuthenticationException.class,
                () -> converter.convert(jwt(null, "staff-1"))
        );
    }

    @Test
    @DisplayName("A token with an unknown role cannot sign in")
    void unknownRoleCannotAuthenticate() {
        assertThrows(
                OAuth2AuthenticationException.class,
                () -> converter.convert(jwt("ROLE_SUPERUSER", "staff-1"))
        );
    }

    private Jwt jwt(String role, String staffId) {
        Jwt.Builder jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("firebase-uid")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (role != null) {
            jwt.claim("role", role);
        }
        if (staffId != null) {
            jwt.claim("staffId", staffId);
        }
        return jwt.build();
    }
}
