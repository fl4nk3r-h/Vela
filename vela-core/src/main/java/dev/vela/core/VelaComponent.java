package dev.vela.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class VelaComponent {
    private final StateManager stateManager = new StateManager(this::notifyStateChanged);
    private Runnable runtimeInvalidation;
    private Thread runtimeThread;
    private final Map<String, VelaComponent> children = new LinkedHashMap<>();
    private final Map<String, SavedValue<?>> saveable = new LinkedHashMap<>();
    private final List<AutoCloseable> resources = new ArrayList<>();
    private Map<String, StateSnapshot> restoredChildren = new LinkedHashMap<>();
    private Set<String> renderingChildren;
    private ViewNode cached;

    protected final <T> MutableState<T> state(T initialValue) {
        return new MutableState<>(initialValue, this::markDirty, this::checkStateThread);
    }

    protected final <T> MutableState<T> saveableState(String key, T initialValue, StateCodec<T> codec) {
        requireKey(key);
        MutableState<T> state = state(Objects.requireNonNull(initialValue, "initialValue"));
        if (saveable.putIfAbsent(key, new SavedValue<>(state, Objects.requireNonNull(codec, "codec"))) != null) {
            throw new IllegalArgumentException("Duplicate saveable state key: " + key);
        }
        return state;
    }

    /** Render a keyed stateful child inside a managed render pass. Keep the child instance stable. */
    protected final ViewNode child(String key, VelaComponent component) {
        requireKey(key);
        Objects.requireNonNull(component, "component");
        if (renderingChildren == null || runtimeInvalidation == null) {
            throw new IllegalStateException("child() requires a VelaRuntime render pass");
        }
        if (component == this || !renderingChildren.add(key)) {
            throw new IllegalArgumentException("Duplicate or recursive child scope: " + key);
        }
        VelaComponent previous = children.get(key);
        if (previous != component) {
            if (previous != null) {
                previous.unmountManaged();
                children.remove(key);
            }
            // Attach first: a component already owned elsewhere must not be torn down here.
            component.attachRuntime(this::notifyStateChanged, runtimeThread);
            children.put(key, component);
            StateSnapshot restored = restoredChildren.remove(key);
            if (restored != null) component.restoreState(restored);
            component.onMount();
        }
        return ViewNode.builder("component").key(key).child(component.renderManaged(true)).build();
    }

    /** Register cancellation/disposal for this mount, for example () -> future.cancel(true). */
    protected final <T extends AutoCloseable> T onDispose(T resource) {
        checkStateThread();
        if (runtimeInvalidation == null) throw new IllegalStateException("Resource requires a mounted scope");
        resources.add(Objects.requireNonNull(resource, "resource"));
        return resource;
    }

    public final StateSnapshot saveState() {
        checkStateThread();
        Map<String, String> values = new LinkedHashMap<>();
        saveable.forEach((key, value) -> values.put(key, value.encode()));
        Map<String, StateSnapshot> snapshots = new LinkedHashMap<>(restoredChildren);
        children.forEach((key, value) -> snapshots.put(key, value.saveState()));
        return new StateSnapshot(values, snapshots);
    }

    public final void restoreState(StateSnapshot snapshot) {
        checkStateThread();
        Objects.requireNonNull(snapshot, "snapshot");
        snapshot.values().forEach((key, value) -> {
            SavedValue<?> entry = saveable.get(key);
            if (entry != null) entry.restore(value);
        });
        restoredChildren = new LinkedHashMap<>(snapshot.children());
        children.forEach((key, value) -> {
            StateSnapshot saved = restoredChildren.remove(key);
            if (saved != null) value.restoreState(saved);
        });
        markDirty();
    }

    protected final void markDirty() {
        checkStateThread();
        stateManager.markDirty();
    }

    public final boolean isDirty() {
        return stateManager.isDirty() || children.values().stream().anyMatch(VelaComponent::isDirty);
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
        cached = null;
    }

    final void detachRuntime() {
        runtimeInvalidation = null;
        runtimeThread = null;
        cached = null;
    }

    final ViewNode renderManaged(boolean parentRendered) {
        boolean ownDirty = stateManager.consumeDirty();
        if (cached == null || ownDirty || parentRendered) {
            renderingChildren = new HashSet<>();
            try {
                ViewNode next = Objects.requireNonNull(render(), "render tree");
                Throwable failure = null;
                for (String key : List.copyOf(children.keySet())) {
                    if (!renderingChildren.contains(key)) {
                        VelaComponent removed = children.remove(key);
                        try { removed.unmountManaged(); }
                        catch (RuntimeException | Error error) { failure = accumulate(failure, error); }
                    }
                }
                rethrow(failure);
                cached = next;
            } catch (RuntimeException | Error error) {
                markDirty();
                throw error;
            } finally {
                renderingChildren = null;
            }
        } else if (isDirty()) {
            cached = refreshChildren(cached);
        }
        return cached;
    }

    private ViewNode refreshChildren(ViewNode node) {
        if (node.type().equals("component") && children.containsKey(node.key())) {
            VelaComponent child = children.get(node.key());
            ViewNode content = child.isDirty() ? child.renderManaged(false) : node.children().get(0);
            return content == node.children().get(0) ? node : node.withChildren(List.of(content));
        }
        List<ViewNode> next = new ArrayList<>(node.children().size());
        boolean changed = false;
        for (ViewNode child : node.children()) {
            ViewNode refreshed = refreshChildren(child);
            next.add(refreshed);
            changed |= refreshed != child;
        }
        return changed ? node.withChildren(next) : node;
    }

    final void unmountManaged() {
        Throwable failure = null;
        for (VelaComponent child : children.values()) {
            try { child.unmountManaged(); }
            catch (RuntimeException | Error error) { failure = accumulate(failure, error); }
        }
        children.clear();
        for (int index = resources.size() - 1; index >= 0; index--) {
            try { resources.get(index).close(); }
            catch (Exception error) { failure = accumulate(failure, new IllegalStateException("Scope disposal failed", error)); }
            catch (Error error) { failure = accumulate(failure, error); }
        }
        resources.clear();
        try { onUnmount(); }
        catch (RuntimeException | Error error) { failure = accumulate(failure, error); }
        finally { detachRuntime(); }
        rethrow(failure);
    }

    private static void requireKey(String key) {
        if (Objects.requireNonNull(key, "key").isBlank()) throw new IllegalArgumentException("Key must not be blank");
    }

    private static Throwable accumulate(Throwable previous, Throwable next) {
        if (previous == null) return next;
        previous.addSuppressed(next);
        return previous;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private record SavedValue<T>(MutableState<T> state, StateCodec<T> codec) {
        String encode() { return Objects.requireNonNull(codec.encode().apply(state.get()), "encoded state"); }
        void restore(String value) { state.set(Objects.requireNonNull(codec.decode().apply(value), "restored state")); }
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
