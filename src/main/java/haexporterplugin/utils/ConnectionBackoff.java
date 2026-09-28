package haexporterplugin.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Per-connection delivery state: when sending is paused, how long the next backoff lasts, and which
 * payloads with events are waiting to be resent. Kept in memory only and safe to use from any thread.
 */
public class ConnectionBackoff {
    static final long INITIAL_BACKOFF_MS = TimeUnit.SECONDS.toMillis(30);
    static final long MAX_BACKOFF_MS = TimeUnit.MINUTES.toMillis(10);
    static final int MAX_QUEUED_PAYLOADS = 50;
    static final long MAX_QUEUE_AGE_MS = TimeUnit.MINUTES.toMillis(10);
    // Lower bound for Retry-After pauses, so "Retry-After: 0" can't cause a tight retry loop
    static final long MIN_RETRY_AFTER_MS = TimeUnit.SECONDS.toMillis(1);

    public enum Outcome {
        SUCCESS,      // 2xx
        UNAUTHORIZED, // 401: token revoked, disable the connection
        GONE,         // 410: endpoint no longer accepts data, disable the connection
        RETRY_AFTER,  // 429 / 503: pause as long as Retry-After asks, back off when it is missing
        BACKOFF,      // other 5xx: exponential backoff
        REJECTED      // anything else: drop the payload, no backoff
    }

    private static final class QueuedPayload {
        private final String payload;
        private final long enqueuedAt;

        private QueuedPayload(String payload, long enqueuedAt) {
            this.payload = payload;
            this.enqueuedAt = enqueuedAt;
        }
    }

    private static final class State {
        // 0 while healthy, otherwise the end of the current (or most recent) pause
        private long pausedUntil;
        private long nextBackoff = INITIAL_BACKOFF_MS;
        private final Deque<QueuedPayload> queue = new ArrayDeque<>();
        // Queued payload that is currently being resent, null when none
        private QueuedPayload inFlight;
    }

    private final LongSupplier clock;
    private final Map<String, State> states = new HashMap<>();

    public ConnectionBackoff() {
        this(System::currentTimeMillis);
    }

    public ConnectionBackoff(LongSupplier clock) {
        this.clock = clock;
    }

    public static Outcome classify(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return Outcome.SUCCESS;
        }
        if (statusCode == 401) {
            return Outcome.UNAUTHORIZED;
        }
        if (statusCode == 410) {
            return Outcome.GONE;
        }
        if (statusCode == 429 || statusCode == 503) {
            return Outcome.RETRY_AFTER;
        }
        if (statusCode >= 500 && statusCode < 600) {
            return Outcome.BACKOFF;
        }
        return Outcome.REJECTED;
    }

    /**
     * Parses a Retry-After header, either delta-seconds or an HTTP-date.
     *
     * @return the moment (epoch millis) to retry at, or null when missing or invalid
     */
    public static Long parseRetryAfter(String headerValue, long nowMillis) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return null;
        }

        String value = headerValue.trim();
        if (value.chars().allMatch(c -> c >= '0' && c <= '9')) {
            try {
                return Math.addExact(nowMillis, Math.multiplyExact(Long.parseLong(value), 1000L));
            } catch (NumberFormatException | ArithmeticException e) {
                return null;
            }
        }

        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public synchronized boolean isPaused(String key) {
        return getPausedUntil(key) > 0;
    }

    /**
     * @return the end of the current pause in epoch millis, or 0 when not paused
     */
    public synchronized long getPausedUntil(String key) {
        State state = states.get(key);
        return state != null && state.pausedUntil > clock.getAsLong() ? state.pausedUntil : 0;
    }

    public synchronized int getQueuedCount(String key) {
        State state = states.get(key);
        if (state == null) {
            return 0;
        }
        prune(state);
        return state.queue.size();
    }

    public synchronized void recordSuccess(String key) {
        State state = states.get(key);
        if (state == null) {
            return;
        }
        state.pausedUntil = 0;
        state.nextBackoff = INITIAL_BACKOFF_MS;
        removeIfIdle(key, state);
    }

    /**
     * Pauses for the current backoff delay and doubles it for next time, up to {@link #MAX_BACKOFF_MS}.
     * A failure reported while already paused (a request that was in flight when the pause began) is ignored.
     *
     * @return true when this is the first failure since the last successful delivery
     */
    public synchronized boolean recordFailure(String key) {
        long now = clock.getAsLong();
        State state = states.computeIfAbsent(key, k -> new State());
        if (state.pausedUntil > now) {
            return false;
        }

        boolean firstFailure = state.pausedUntil == 0;
        state.pausedUntil = now + state.nextBackoff;
        state.nextBackoff = Math.min(state.nextBackoff * 2, MAX_BACKOFF_MS);
        return firstFailure;
    }

    /**
     * Pauses until the moment the server asked for, kept between {@link #MIN_RETRY_AFTER_MS} and
     * {@link #MAX_BACKOFF_MS} from now. Does not advance the exponential backoff.
     *
     * @return true when this is the first failure since the last successful delivery
     */
    public synchronized boolean recordRetryAfter(String key, long untilMillis) {
        long now = clock.getAsLong();
        State state = states.computeIfAbsent(key, k -> new State());
        boolean firstFailure = state.pausedUntil == 0;
        long until = Math.max(now + MIN_RETRY_AFTER_MS, Math.min(untilMillis, now + MAX_BACKOFF_MS));
        state.pausedUntil = Math.max(state.pausedUntil, until);
        return firstFailure;
    }

    /**
     * Queues a payload for resending, dropping the oldest ones beyond {@link #MAX_QUEUED_PAYLOADS}
     * and any older than {@link #MAX_QUEUE_AGE_MS}.
     */
    public synchronized void enqueue(String key, String payload) {
        State state = states.computeIfAbsent(key, k -> new State());
        prune(state);
        state.queue.addLast(new QueuedPayload(payload, clock.getAsLong()));
        while (state.queue.size() > MAX_QUEUED_PAYLOADS) {
            state.queue.removeFirst();
        }
    }

    /**
     * Starts resending the oldest queued payload. Pair every non-null result with {@link #completeDrain}.
     *
     * @return the payload to send, or null when paused, nothing is queued or a resend is already in flight
     */
    public synchronized String beginDrain(String key) {
        State state = states.get(key);
        if (state == null || state.inFlight != null || state.pausedUntil > clock.getAsLong()) {
            return null;
        }
        prune(state);
        state.inFlight = state.queue.peekFirst();
        return state.inFlight != null ? state.inFlight.payload : null;
    }

    /**
     * Finishes the resend started by {@link #beginDrain}: a delivered payload is removed from the queue,
     * otherwise it stays at the head to be retried after the next pause.
     */
    public synchronized void completeDrain(String key, boolean delivered) {
        State state = states.get(key);
        if (state == null || state.inFlight == null) {
            return;
        }
        if (delivered) {
            // Removed by identity: it may have been pushed out of the queue while in flight
            state.queue.remove(state.inFlight);
        }
        state.inFlight = null;
        removeIfIdle(key, state);
    }

    public synchronized void clear(String key) {
        states.remove(key);
    }

    public synchronized void clearAll() {
        states.clear();
    }

    private void prune(State state) {
        long cutoff = clock.getAsLong() - MAX_QUEUE_AGE_MS;
        while (!state.queue.isEmpty() && state.queue.peekFirst().enqueuedAt < cutoff) {
            state.queue.removeFirst();
        }
    }

    private void removeIfIdle(String key, State state) {
        if (state.pausedUntil == 0 && state.inFlight == null && state.queue.isEmpty()) {
            states.remove(key);
        }
    }
}
