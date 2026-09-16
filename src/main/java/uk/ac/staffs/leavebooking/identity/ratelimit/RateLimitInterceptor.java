package uk.ac.staffs.leavebooking.identity.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import uk.ac.staffs.leavebooking.identity.ratelimit.FixedWindowRateLimiter.Decision;
import uk.ac.staffs.leavebooking.identity.ratelimit.FixedWindowRateLimiter.RateLimitKey;
import uk.ac.staffs.leavebooking.identity.security.CurrentUser;
import uk.ac.staffs.leavebooking.identity.security.SecurityErrorWriter;

import java.io.IOException;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String RATE_LIMIT_MESSAGE = "Too many requests. Please retry later.";

    private static final String APPLIED_KEY_ATTRIBUTE =
            RateLimitInterceptor.class.getName() + ".APPLIED_KEY";
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimitProperties properties;
    private final FixedWindowRateLimiter rateLimiter;
    private final CurrentUser currentUser;
    private final SecurityErrorWriter errorWriter;

    @Autowired
    public RateLimitInterceptor(
            RateLimitProperties properties,
            CurrentUser currentUser,
            SecurityErrorWriter errorWriter
    ) {
        this(properties, new FixedWindowRateLimiter(), currentUser, errorWriter);
    }

    RateLimitInterceptor(
            RateLimitProperties properties,
            FixedWindowRateLimiter rateLimiter,
            CurrentUser currentUser,
            SecurityErrorWriter errorWriter
    ) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.currentUser = currentUser;
        this.errorWriter = errorWriter;
    }

    void clearBuckets() {
        rateLimiter.clear();
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (!properties.isEnabled()) {
            return true;
        }

        String endpointPattern = endpointPattern(request);
        boolean login = "POST".equals(request.getMethod()) && "/auth/login".equals(endpointPattern);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!login && !isAuthenticated(authentication)) {
            return true;
        }

        String requester = login
                ? "ip:" + request.getRemoteAddr()
                : authenticatedRequester(authentication);
        RateLimitProperties.Limit limit = login ? properties.getLogin() : properties.getApi();
        RateLimitKey key = new RateLimitKey(requester, request.getMethod(), endpointPattern);
        Decision decision = rateLimiter.tryAcquire(
                key,
                limit.getCapacity(),
                limit.getWindowSeconds()
        );
        if (decision.allowed()) {
            request.setAttribute(APPLIED_KEY_ATTRIBUTE, key);
            return true;
        }

        LOGGER.warn(
                "RATE_LIMIT outcome=429 method={} endpoint={} requester={} remoteIp={}",
                request.getMethod(),
                endpointPattern,
                requester,
                request.getRemoteAddr()
        );
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        errorWriter.write(
                request,
                response,
                HttpStatus.TOO_MANY_REQUESTS.value(),
                RATE_LIMIT_EXCEEDED,
                RATE_LIMIT_MESSAGE
        );
        return false;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        if (response.getStatus() == HttpServletResponse.SC_FORBIDDEN
                && request.getAttribute(APPLIED_KEY_ATTRIBUTE) instanceof RateLimitKey key) {
            rateLimiter.release(key);
        }
    }

    private String authenticatedRequester(Authentication authentication) {
        return currentUser.staffId(authentication)
                .map(staffId -> "staff:" + staffId)
                .or(() -> currentUser.firebaseUid(authentication).map(uid -> "uid:" + uid))
                .orElseGet(() -> "principal:" + authentication.getName());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String endpointPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? request.getRequestURI() : pattern.toString();
    }
}
