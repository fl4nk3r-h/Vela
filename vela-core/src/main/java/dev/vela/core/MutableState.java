package dev.vela.core;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class MutableState<T> {
    private final Runnable invalidate;
    private final Runnable checkThread;
    private T value;

    MutableState(T initialValue, Runnable invalidate, Runnable checkThread) {
        value = initialValue;
        this.invalidate = Objects.requireNonNull(invalidate, "invalidate");
        this.checkThread = Objects.requireNonNull(checkThread, "checkThread");
    }

    public T get() {
        checkThread.run();
        return value;
    }

    public boolean set(T value) {
        checkThread.run();
        if (Objects.equals(this.value, value)) {
            return false;
        }
        this.value = value;
        invalidate.run();
        return true;
    }

    public boolean update(UnaryOperator<T> update) {
        checkThread.run();
        return set(Objects.requireNonNull(update, "update").apply(value));
    }
}
