package uk.ac.staffs.leavebooking.identity.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class FixedWindowRateLimiter {
    private final Clock clock;
    private final Map<RateLimitKey, WindowCounter> counters = new HashMap<>();

    public FixedWindowRateLimiter() {
        this(Clock.systemUTC());
    }

    FixedWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized Decision tryAcquire(
            RateLimitKey key,
            int capacity,
            int windowSeconds
    ) {
        if (capacity <= 0 || windowSeconds <= 0) {
            throw new IllegalArgumentException("Rate-limit capacity and window must be positive");
        }

        Instant now = clock.instant();
        WindowCounter counter = counters.get(key);
        if (counter == null || !now.isBefore(counter.startedAt.plusSeconds(windowSeconds))) {
            counters.put(key, new WindowCounter(now, 1));
            return Decision.permit();
        }
        if (counter.count < capacity) {
            counter.count++;
            return Decision.permit();
        }

        long remainingMillis = Duration.between(
                now,
                counter.startedAt.plusSeconds(windowSeconds)
        ).toMillis();
        long retryAfter = Math.max(1, (remainingMillis + 999) / 1000);
        return Decision.deny(retryAfter);
    }

    public synchronized void release(RateLimitKey key) {
        WindowCounter counter = counters.get(key);
        if (counter != null && counter.count > 0) {
            counter.count--;
        }
    }

    synchronized void clear() {
        counters.clear();
    }

    public record RateLimitKey(String requester, String method, String endpointPattern) {
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
        static Decision permit() {
            return new Decision(true, 0);
        }

        static Decision deny(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }
    }

    private static final class WindowCounter {
        private final Instant startedAt;
        private int count;

        private WindowCounter(Instant startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
