package be.submanifold.pentelive.net;

/**
 * Immutable outcome of a network operation: either a value ({@link #ok})
 * or a {@link Failure} ({@link #fail}). Exactly one of {@link #value} /
 * {@link #failure} is non-null for a failure; {@link #isOk()} keys off
 * {@code failure == null} so {@code ok(null)} (e.g. {@code Result<Void>})
 * is still considered ok.
 */
public final class Result<T> {

    /** Categories of network failure, mapped to caller-visible handling. */
    public enum Reason {
        NETWORK,
        AUTH_EXPIRED,
        INVALID_CREDENTIALS,
        SERVER,
        PARSE
    }

    /** Immutable description of why an operation failed. */
    public static final class Failure {
        public final Reason reason;
        public final int httpCode;
        public final Throwable cause;

        public Failure(Reason reason, int httpCode, Throwable cause) {
            this.reason = reason;
            this.httpCode = httpCode;
            this.cause = cause;
        }
    }

    public final T value;
    public final Failure failure;

    private Result(T value, Failure failure) {
        this.value = value;
        this.failure = failure;
    }

    public boolean isOk() {
        return failure == null;
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> fail(Failure failure) {
        return new Result<>(null, failure);
    }
}
