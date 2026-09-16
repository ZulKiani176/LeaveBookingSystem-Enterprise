package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FirebaseJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String roleClaim = jwt.getClaimAsString("role");
        try {
            Role role = Role.fromAuthority(roleClaim);
            return new JwtAuthenticationToken(
                    jwt,
                    List.of(new SimpleGrantedAuthority(role.getAuthority())),
                    jwt.getSubject()
            );
        } catch (IllegalArgumentException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN),
                    "Firebase token has no valid role claim",
                    exception
            );
        }
    }
}
