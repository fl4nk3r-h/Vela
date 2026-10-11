package dev.vela.core;

@FunctionalInterface
public interface FrameScheduler {
    // Queue the callback for a later turn on the runtime's owning thread.
    void schedule(Runnable callback);
}
