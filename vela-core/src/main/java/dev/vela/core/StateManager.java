package dev.vela.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StateManager {
    private final Runnable onDirty;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public StateManager(Runnable onDirty) {
        this.onDirty = Objects.requireNonNull(onDirty, "onDirty");
    }

    public void markDirty() {
        if (dirty.compareAndSet(false, true)) {
            onDirty.run();
        }
    }

    public boolean consumeDirty() {
        return dirty.getAndSet(false);
    }

    public boolean isDirty() {
        return dirty.get();
    }
}