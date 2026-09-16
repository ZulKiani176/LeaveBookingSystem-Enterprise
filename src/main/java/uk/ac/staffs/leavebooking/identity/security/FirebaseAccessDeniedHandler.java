package uk.ac.staffs.leavebooking.identity.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FirebaseAccessDeniedHandler implements AccessDeniedHandler {
    public static final String ACCESS_DENIED = "ACCESS_DENIED";

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseAccessDeniedHandler.class);

    private final SecurityErrorWriter errorWriter;

    public FirebaseAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication == null ? "anonymous" : authentication.getName();
        String authorities = authentication == null
                ? "[]"
                : authentication.getAuthorities().toString();
        LOG.warn(
                "SECURITY outcome=403 method={} uri={} principal={} authorities={} remoteIp={}",
                request.getMethod(),
                request.getRequestURI(),
                principal,
                authorities,
                request.getRemoteAddr()
        );
        errorWriter.write(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                ACCESS_DENIED,
                "Access is denied"
        );
    }
}
