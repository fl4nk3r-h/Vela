package dev.vela.core;

public abstract class VelaComponent {
    private final StateManager stateManager = new StateManager(this::onStateChanged);

    protected final void markDirty() {
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

    public abstract ViewNode render();
}