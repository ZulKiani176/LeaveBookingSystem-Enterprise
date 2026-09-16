package uk.ac.staffs.leavebooking.identity.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.identity.ratelimit.FixedWindowRateLimiter.Decision;
import uk.ac.staffs.leavebooking.identity.ratelimit.FixedWindowRateLimiter.RateLimitKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Request limits")
class FixedWindowRateLimiterTests {
    private MutableClock clock;
    private FixedWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-25T12:00:00Z"));
        limiter = new FixedWindowRateLimiter(clock);
    }

    @Test
    @DisplayName("Requests through capacity are allowed and the next is blocked")
    void capacityIsEnforced() {
        RateLimitKey key = key("staff:1", "/api/staff/{staffMemberId}/leave-requests");

        assertTrue(limiter.tryAcquire(key, 2, 60).allowed());
        assertTrue(limiter.tryAcquire(key, 2, 60).allowed());
        Decision blocked = limiter.tryAcquire(key, 2, 60);

        assertFalse(blocked.allowed());
        assertEquals(60, blocked.retryAfterSeconds());
    }

    @Test
    @DisplayName("A new fixed window restores capacity without sleeping")
    void newWindowRestoresCapacity() {
        RateLimitKey key = key("staff:1", "/api/staff/{staffMemberId}/leave-requests");
        limiter.tryAcquire(key, 1, 60);
        assertFalse(limiter.tryAcquire(key, 1, 60).allowed());

        clock.advance(Duration.ofSeconds(60));

        assertTrue(limiter.tryAcquire(key, 1, 60).allowed());
    }

    @Test
    @DisplayName("Different authenticated identities have independent buckets")
    void identitiesAreIndependent() {
        RateLimitKey first = key("uid:first", "/api/leave-requests");
        RateLimitKey second = key("uid:second", "/api/leave-requests");
        limiter.tryAcquire(first, 1, 60);

        assertFalse(limiter.tryAcquire(first, 1, 60).allowed());
        assertTrue(limiter.tryAcquire(second, 1, 60).allowed());
    }

    @Test
    @DisplayName("Different matched endpoint patterns have independent buckets")
    void endpointPatternsAreIndependent() {
        RateLimitKey requests = key("staff:1", "/api/staff/{staffMemberId}/leave-requests");
        RateLimitKey allowances = key("staff:1", "/api/staff/{staffMemberId}/leave-allowances");
        limiter.tryAcquire(requests, 1, 60);

        assertFalse(limiter.tryAcquire(requests, 1, 60).allowed());
        assertTrue(limiter.tryAcquire(allowances, 1, 60).allowed());
    }

    @Test
    @DisplayName("Releasing a forbidden attempt restores the consumed permit")
    void releasedPermitCanBeReused() {
        RateLimitKey key = key("staff:1", "/api/staff/{staffMemberId}/leave-requests");
        limiter.tryAcquire(key, 1, 60);

        limiter.release(key);

        assertTrue(limiter.tryAcquire(key, 1, 60).allowed());
    }

    private RateLimitKey key(String requester, String endpoint) {
        return new RateLimitKey(requester, "GET", endpoint);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
