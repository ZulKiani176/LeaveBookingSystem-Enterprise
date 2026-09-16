package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.List;

public class WithMockFirebaseUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockFirebaseUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockFirebaseUser annotation) {
        Instant issuedAt = Instant.now();
        Jwt.Builder jwt = Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "RS256")
                .subject(annotation.uid())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .claim("role", annotation.role().getAuthority());
        if (!annotation.staffId().isBlank()) {
            jwt.claim("staffId", annotation.staffId());
        }

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt.build(),
                List.of(new SimpleGrantedAuthority(annotation.role().getAuthority())),
                annotation.uid()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
