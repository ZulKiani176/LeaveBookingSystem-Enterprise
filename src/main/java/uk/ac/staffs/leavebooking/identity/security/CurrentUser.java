package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("currentUser")
public class CurrentUser {
    public Optional<String> firebaseUid(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.ofNullable(token.getToken().getSubject());
        }
        return Optional.empty();
    }

    public Optional<String> staffId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            String staffId = token.getToken().getClaimAsString("staffId");
            return staffId == null || staffId.isBlank()
                    ? Optional.empty()
                    : Optional.of(staffId);
        }
        return Optional.empty();
    }

    public boolean isOwner(Authentication authentication, String staffMemberId) {
        return staffMemberId != null
                && staffId(authentication).map(staffMemberId::equals).orElse(false);
    }

    public boolean hasRole(Authentication authentication, Role role) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.getAuthority().equals(authority.getAuthority()));
    }

    public boolean hasAnyRole(Authentication authentication, Role... roles) {
        for (Role role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }
}
