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

Nested component scopes, saveable state, process restoration, and lifecycle-bound effects remain planned.

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

`update` runs for retained nodes even if their visible props are equal. This ensures event callbacks capture current application state. A backend can skip unchanged visual properties, but must still refresh its event bindings. Child-order updates are omitted entirely when handle order is unchanged.

The Android showcase currently uses its original recursive renderer and manual scheduler. It has not yet been adapted to this contract, so native Android view retention is a separate roadmap milestone.

## Identity And Reconciliation

`MountedTree<H>` reconciles trees without requiring component state or a scheduler. A handle can be retained only when both type and key match.

Keys identify children within one parent. Keyed children retain their handles across reordering. Unkeyed children match by position. A type/key change creates a replacement. Moving a keyed node to a different parent does not preserve its handle.

Duplicate sibling keys are rejected before the incoming tree mutates backend state. Removed children are detached before disposal. Teardown attempts remaining handles even if one disposal throws.

The core uses keyed maps and ordered lists for linear matching at each sibling group. Backend operations may have their own costs. This is full root-tree traversal with retained hosts, not fine-grained or sublinear recomposition.

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
