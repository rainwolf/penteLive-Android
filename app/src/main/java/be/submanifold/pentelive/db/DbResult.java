package be.submanifold.pentelive.db;

public final class DbResult<T> {
    public enum Kind { OK, UNAUTHORIZED, SUBSCRIPTION_REQUIRED, BLOCKED, FAILED }

    public final Kind kind;
    public final T value;

    private DbResult(Kind kind, T value) {
        this.kind = kind;
        this.value = value;
    }

    public static <T> DbResult<T> ok(T value) {
        return new DbResult<>(Kind.OK, value);
    }

    public static <T> DbResult<T> of(Kind kind) {
        return new DbResult<>(kind, null);
    }
}
