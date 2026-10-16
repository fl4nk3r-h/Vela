# Runtime Guide

Use this page to build observable state and mount a Vela component through a retained renderer backend.

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Application developers and renderer authors |
| Owner | Vela maintainers |
| Scope | `MutableState`, `VelaRuntime`, `FrameScheduler`, `MountedTree`, and renderer contracts |

## Observable State

Create state inside a `VelaComponent` using `state(initialValue)`:

```java
private final MutableState<Integer> count = state(0);

public void increment() {
    count.update(value -> value + 1);
}

public void reset() {
    count.set(0);
}
```

Read the current value with `count.get()`. `set` and `update` return whether the value changed. Equality uses `Objects.equals`; assigning an equal value does not mark the component dirty. Null values are supported.

Use immutable values or replace mutable collections with new values. Mutating an object already stored in state does not invoke `set` and cannot be observed. This first implementation invalidates the owning component as a whole; it does not track individual state reads or subscribe child scopes.

Do not annotate a final `MutableState` field with `@State`. Observable holders do not require the processor. Legacy `@State` fields still require manual `markDirty()` calls, and the existing compiler rules still reject final annotated fields.

## Mount A Component

The console sample uses the built-in retained text backend:

```java
CounterScreen screen = new CounterScreen();
TextRendererBackend backend = new TextRendererBackend();
Deque<Runnable> frames = new ArrayDeque<>();

try (VelaRuntime<TextRendererBackend.Node> runtime =
        new VelaRuntime<>(screen, backend, frames::addLast)) {
    runtime.mount();
    System.out.println(backend.output());

    TextRendererBackend.Node button = runtime.root().children().get(2);
    button.dispatch("click");
    button.dispatch("click");

    while (!frames.isEmpty()) {
        frames.removeFirst().run();
    }
    System.out.println(backend.output());
}
```

Both clicks update state immediately, but they queue only one render callback. The second render retains the existing button handle. Closing the runtime detaches the root, releases its handles, and stops disposed text nodes from dispatching events.

The existing `VelaRenderer.render(ViewNode)` diagnostic API remains available for direct tree inspection.

## Scheduler Contract

`FrameScheduler.schedule(Runnable)` must defer the callback and execute it on the runtime's owning thread. Inline execution is rejected. A console queue is useful for tests; a platform adapter should bind this contract to its UI frame loop.

`VelaRuntime` batches invalidations while one callback is pending. `flush()` renders dirty state immediately on the owning thread and invalidates any stale queued callback. `requestRender()` explicitly marks the root dirty and schedules an update.

An update exception leaves the component dirty and is surfaced to the caller. It is not retried in an automatic loop. After correcting a transient failure, use `requestRender()` or `flush()` to retry. Backend mutation is not transactional; recreate the runtime if a backend failure leaves its host tree inconsistent.

Closing the runtime makes previously queued callbacks no-ops. The generic scheduler has no physical cancellation API yet, so integrations may still retain an inert callback until their next queue/frame turn.

## Threading And Lifecycle

Create, mount, access, flush, and close a runtime on one thread. Mounted component state reads and writes enforce that thread before touching the value. Unmounted state is not thread-safe; use one thread consistently and marshal background results to the UI thread.

A root component can be attached to only one runtime at a time. The runtime calls `onMount()` before its initial render and `onUnmount()` during teardown. A component can be mounted in a new runtime after teardown, retaining its stored values. A closed runtime itself cannot be mounted again.

Do not manually consume dirty flags or add a second scheduling owner to a component managed by `VelaRuntime`. `onStateChanged()` remains available for compatibility with manual integrations. Mount/unmount hooks do not run when a caller uses `render()` directly.

### Stateful Child Scopes

Use `child(key, stableComponentInstance)` inside a managed `render()` to attach child state and lifecycle to the runtime:

```java
private final CounterScreen counter = new CounterScreen();

@Override public ViewNode render() {
    return VelaColumn.of(child("counter", counter), VelaText.of("Other content"));
}
```

Keep child instances stable. Keys are unique within their owning component. A child mounts once, updates independently, and unmounts when omitted or replaced. Calling `child.render()` directly bypasses these facilities. `child(...)` requires a runtime-managed render pass and returns a transparent component node.

A child invalidation refreshes its description while retaining its parent's and unaffected siblings' cached descriptions. When a parent itself renders, child descriptions are refreshed too, so changed inputs and callbacks remain current. This is explicit component-scoped invalidation; arbitrary state reads across unrelated owners are not dependency-tracked.

### Saveable State And Scoped Resources

Declare explicit state codecs for values that must survive recreation:

```java
private final MutableState<Integer> count = saveableState("count", 0, StateCodec.INTEGER);
```

`saveState()` returns a portable `StateSnapshot`; `restoreState(snapshot)` applies values and pending keyed child snapshots. Restore before mounting for initial restoration. Built-in codecs support strings, integers, longs, finite doubles, and booleans; custom `StateCodec<T>` values supply encode/decode functions. Saveable initial values and decoded values must be non-null. Ordinary `state(...)` still supports null and remains transient.

Register mount-owned cancellation in `onMount()` with `onDispose(autoCloseable)`. Resources are closed in reverse registration order; cleanup continues after failures. Use `runtime.dispatch(action)` to deliver asynchronous results on the owner thread when the scheduler accepts cross-thread posting. Queued and late results are dropped after closure. `AndroidFrameScheduler` supplies that thread-safe integration and physical callback cancellation. Native Bundle and view restoration are documented in [Android backend](android-backend.md).

## Renderer Backend

`RendererBackend<H>` owns platform objects identified by handles of type `H`:

| Method | Responsibility |
| --- | --- |
| `create(ViewNode)` | Create one host object and apply initial props/events; do not recursively mount its children |
| `update(handle, previous, next)` | Patch current properties and rebind current event callbacks |
| `setChildren(parent, children)` | Apply the ordered managed children while retaining surviving host objects |
| `setRoot(root)` | Attach the root to the backend container; null detaches it |
| `dispose(handle)` | Release resources and remove event listeners after detachment |

Give each runtime its own backend/container. The child list supplied to `setChildren` is immutable. Backend-specific internal widget children are distinct from the managed children described by Vela nodes.

`update` runs for retained nodes with a new description even if their visible props are equal. This ensures event callbacks capture current application state. A backend can skip unchanged visual properties, but must still refresh its event bindings. Child-order updates are omitted entirely when handle order is unchanged.

The shared `vela-android` backend implements this contract. Daymark mounts it through `VelaRuntime` and `AndroidFrameScheduler`; it preserves native controls between state updates. See [Android backend](android-backend.md).

## Identity And Reconciliation

`MountedTree<H>` reconciles trees without requiring component state or a scheduler. A handle can be retained only when both type and key match.

Keys identify children within one parent. Keyed children retain their handles across reordering. Unkeyed children match by position. A type/key change creates a replacement. Moving a keyed node to a different parent does not preserve its handle.

Duplicate sibling keys are rejected before the incoming tree mutates backend state. Removed children are detached before disposal. Teardown attempts remaining handles even if one disposal throws.

The core uses keyed maps and ordered lists for linear matching at each changed sibling group. Identical immutable node instances skip patching and cached key validation. Managed child scopes reuse unaffected descriptions. Eager trees created afresh still require traversal; use component boundaries and `VelaLazyList`/`VelaLazyGrid` for large collections. There is no automatic global state-read dependency tracking or separate layout/paint invalidation system.

## Verification

```bash
./gradlew test :app:run --no-configuration-cache
```

Tests cover handle retention, keyed ordering, insertions/removals, type replacement, callback refresh, duplicate-key validation, cleanup, state equality, batching, thread checks, and stale callbacks.

## Related Pages

- [`usage-guide.md`](usage-guide.md)
- [`framework-gaps.md`](framework-gaps.md)
- [`project-structure.md`](project-structure.md)
- [`README.md`](../README.md)
