package uk.ac.staffs.leavebooking.identity.security;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Security roles")
class RoleTests {
    @Test
    @DisplayName("Each role has the correct authority name")
    void everyRoleExposesSpringAuthority() {
        assertEquals("ROLE_STAFF", Role.STAFF.getAuthority());
        assertEquals("ROLE_MANAGER", Role.MANAGER.getAuthority());
        assertEquals("ROLE_HR", Role.HR.getAuthority());
        assertEquals("ROLE_ADMIN", Role.ADMIN.getAuthority());
    }

    @Test
    @DisplayName("Roles can be read from their names or authority names")
    void roleNameAndAuthorityBothParse() {
        assertEquals(Role.STAFF, Role.fromString(" staff "));
        assertEquals(Role.MANAGER, Role.fromString("ROLE_MANAGER"));
        assertEquals(Role.HR, Role.fromAuthority("ROLE_HR"));
        assertEquals(Role.ADMIN, Role.fromAuthority("role_admin"));
    }

    @Test
    @DisplayName("Missing, blank and unknown roles are rejected")
    void missingAndUnknownRolesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Role.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("  "));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("SUPERUSER"));
    }
}
