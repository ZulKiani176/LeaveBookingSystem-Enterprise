package uk.ac.staffs.leavebooking.identity.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FirebaseAuthenticationEntryPoint implements AuthenticationEntryPoint {
    public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseAuthenticationEntryPoint.class);

    private final SecurityErrorWriter errorWriter;

    public FirebaseAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        boolean bearerSupplied = request.getHeader("Authorization") != null;
        String code = bearerSupplied ? INVALID_TOKEN : AUTHENTICATION_REQUIRED;
        String message = bearerSupplied
                ? "The bearer token is invalid or expired"
                : "Authentication is required";
        LOG.warn(
                "SECURITY outcome=401 method={} uri={} principal=anonymous authorities=[] remoteIp={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr()
        );
        errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, code, message);
    }
}
