package dev.vela.core;

public abstract class VelaComponent {
    private final StateManager stateManager = new StateManager(this::notifyStateChanged);
    private Runnable runtimeInvalidation;
    private Thread runtimeThread;

    protected final <T> MutableState<T> state(T initialValue) {
        return new MutableState<>(initialValue, this::markDirty, this::checkStateThread);
    }

    protected final void markDirty() {
        checkStateThread();
        stateManager.markDirty();
    }

    public final boolean isDirty() {
        return stateManager.isDirty();
    }

    public final boolean consumeDirty() {
        return stateManager.consumeDirty();
    }

    protected void onStateChanged() {
        // Override in integrations that can schedule a re-render.
    }

    protected void onMount() {
    }

    protected void onUnmount() {
    }

    final void attachRuntime(Runnable invalidate, Thread thread) {
        if (runtimeInvalidation != null) {
            throw new IllegalStateException("Component is already mounted in a runtime");
        }
        runtimeInvalidation = invalidate;
        runtimeThread = thread;
    }

    final void detachRuntime() {
        runtimeInvalidation = null;
        runtimeThread = null;
    }

    private void checkStateThread() {
        if (runtimeThread != null && Thread.currentThread() != runtimeThread) {
            throw new IllegalStateException("Mounted component state must be accessed on its runtime thread");
        }
    }

    private void notifyStateChanged() {
        if (runtimeInvalidation != null) {
            runtimeInvalidation.run();
        }
        onStateChanged();
    }

    public abstract ViewNode render();
}
