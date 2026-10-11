package dev.vela.core;

import java.util.Objects;

public final class VelaRuntime<H> implements AutoCloseable {
    private final VelaComponent component;
    private final MountedTree<H> tree;
    private final FrameScheduler scheduler;
    private final Thread owner = Thread.currentThread();
    private boolean mounted;
    private boolean attached;
    private boolean closed;
    private boolean rendering;
    private boolean queued;
    private boolean scheduling;
    private long generation;

    public VelaRuntime(VelaComponent component, RendererBackend<H> backend, FrameScheduler scheduler) {
        this.component = Objects.requireNonNull(component, "component");
        tree = new MountedTree<>(backend);
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void mount() {
        checkThread();
        if (closed || mounted) {
            throw new IllegalStateException("Runtime can only be mounted once");
        }
        component.attachRuntime(this::invalidate, owner);
        attached = true;
        mounted = true;
        rendering = true;
        try {
            component.onMount();
            renderPass();
        } catch (RuntimeException | Error failure) {
            try {
                close();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        } finally {
            rendering = false;
        }
        if (component.isDirty()) {
            invalidate();
        }
    }

    public H root() {
        checkThread();
        return tree.root();
    }

    public void requestRender() {
        checkMounted();
        component.markDirty();
        invalidate();
    }

    public void flush() {
        checkMounted();
        if (rendering) {
            throw new IllegalStateException("Cannot flush during a render pass");
        }
        generation++;
        queued = false;
        if (!component.isDirty()) {
            return;
        }
        rendering = true;
        try {
            renderPass();
        } finally {
            rendering = false;
        }
        if (component.isDirty()) {
            invalidate();
        }
    }

    private void renderPass() {
        component.consumeDirty();
        try {
            tree.render(Objects.requireNonNull(component.render(), "render tree"));
        } catch (RuntimeException | Error failure) {
            try {
                component.markDirty();
            } catch (RuntimeException | Error invalidationFailure) {
                failure.addSuppressed(invalidationFailure);
            }
            throw failure;
        }
    }

    private void invalidate() {
        checkThread();
        if (!mounted || rendering || queued) {
            return;
        }
        queued = true;
        long ticket = ++generation;
        scheduling = true;
        try {
            scheduler.schedule(() -> {
                if (!mounted || ticket != generation) {
                    return;
                }
                if (scheduling) {
                    throw new IllegalStateException("FrameScheduler must defer callbacks");
                }
                checkThread();
                queued = false;
                flush();
            });
        } catch (RuntimeException | Error failure) {
            queued = false;
            generation++;
            throw failure;
        } finally {
            scheduling = false;
        }
    }

    private void checkThread() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("Runtime must be accessed on its owning thread");
        }
    }

    private void checkMounted() {
        checkThread();
        if (!mounted) {
            throw new IllegalStateException("Runtime is not mounted");
        }
    }

    @Override
    public void close() {
        checkThread();
        if (closed) {
            return;
        }
        closed = true;
        mounted = false;
        queued = false;
        generation++;
        try {
            tree.close();
        } finally {
            if (attached) {
                try {
                    component.onUnmount();
                } finally {
                    component.detachRuntime();
                    attached = false;
                }
            }
        }
    }
}
