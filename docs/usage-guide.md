# Usage Guide

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Application developers and framework contributors |
| Owner | Vela maintainers |
| Scope | Public runtime APIs available in `vela-core` and the sample `app` module |

## Overview

Vela is a small Java UI experiment inspired by declarative component systems such as Jetpack Compose. A screen is modeled as a `VelaComponent`; each render pass returns an immutable `ViewNode` tree; state mutations mark the component dirty; and a renderer turns the tree into output.

The core includes diagnostic text rendering and a retained renderer contract. The shared `vela-android` library implements that contract with native widgets and a frame scheduler; Daymark consumes it. Desktop, web, and terminal UI backends remain planned.

## Mental Model

| Compose-like concept | Vela equivalent today | Current behavior |
| --- | --- | --- |
| Composable function or component | `VelaComponent` subclass | Owns state and returns a `ViewNode` from `render()` |
| Mutable state | `MutableState<T>` created by `state(...)` | Equal writes are skipped; changed values invalidate the owner automatically |
| Composition tree | `ViewNode` | Immutable node with type, optional stable key, text, string props, children, and event handlers |
| Recomposition request | Dirty flag plus `VelaRuntime` | Root invalidations are batched through a deferred `FrameScheduler` |
| UI element factories | `dev.vela.components` catalog | Layout, content, input, feedback, navigation, overlay, and data-display nodes |
| Compose modifier | `Modifier` | Immutable sizing, spacing, appearance, layout, interaction, and semantic metadata |
| Renderer | `VelaRenderer` | Produces an indented text representation of a tree |
| Mounted tree | `MountedTree` and `RendererBackend` | Retain backend handles using type/key identity and refresh event callbacks |

## Install In A Module

Inside this workspace, add the core module as an implementation dependency:

```gradle
dependencies {
    implementation project(':vela-core')
}
```

Enable compile-time validation for `@State` by adding the compiler module as an annotation processor:

```gradle
dependencies {
    annotationProcessor project(':vela-compiler')
}
```

The sample `app` module already has both dependencies configured.

## Create A Component

Create a class that extends `VelaComponent` and override `render()`:

```java
package dev.vela.app;

import dev.vela.components.VelaColumn;
import dev.vela.components.VelaText;
import dev.vela.core.VelaComponent;
import dev.vela.core.ViewNode;

public final class GreetingScreen extends VelaComponent {
    @Override
    public ViewNode render() {
        return VelaColumn.of(
            VelaText.of("Greeting"),
            VelaText.of("Hello from Vela")
        );
    }
}
```

`render()` should describe the current UI from current component state. Change observable state in event handlers or explicit component methods.

## Compose Rich UI

Components combine into immutable trees and accept shared modifiers:

```java
ViewNode profile = VelaCard.of(
    ComponentVariant.OUTLINED,
    Modifier.none().fillMaxWidth().padding(16).cornerRadius(8),
    VelaRow.of(
        Arrangement.SPACE_BETWEEN,
        Alignment.CENTER,
        12,
        Modifier.none(),
        VelaAvatar.initials("AV", 48, Modifier.none()),
        VelaText.of(
            "Aritra",
            TextStyle.defaultStyle().fontSize(18).fontWeight(700),
            Modifier.none().weight(1)
        ),
        VelaButton.of("Edit", this::openEditor)
    )
);
```

See the [component reference](components.md) for the complete catalog and signatures.

## Add State

Use an observable holder for automatic invalidation:

```java
import dev.vela.core.MutableState;

public final class CounterScreen extends VelaComponent {
    private final MutableState<Integer> count = state(0);

    public void increment() {
        count.update(value -> value + 1);
    }

    @Override
    public ViewNode render() {
        return VelaColumn.of(
            VelaText.of("Counter"),
            VelaText.of("Current value: " + count.get()),
            VelaButton.of("Increment", this::increment)
        );
    }
}
```

`MutableState.set` and `update` invalidate only when `Objects.equals` detects a changed value. The final holder does not need `@State`; use immutable values and replace them rather than mutating their contents in place.

Legacy annotation constraints still apply to manually managed fields:

| Rule | Reason |
| --- | --- |
| `@State` can only be used on fields | Enforced by `StateProcessor` |
| `@State` fields cannot be `final` | Enforced by `StateProcessor` because state must be mutable |
| `@State` fields must belong to a `VelaComponent` subclass | Prevents state annotations on objects that cannot participate in invalidation |
| Direct changes to annotated fields must call `markDirty()` manually | Annotation validation does not intercept writes |
| Mounted state must be accessed on the runtime thread | Enforced before observable values are read or written |

See the [runtime guide](runtime-guide.md) for scheduling, lifecycle, backend contracts, and retained rendering.

## Render A Component

Use `VelaRenderer` for direct diagnostic rendering:

```java
VelaRenderer renderer = new VelaRenderer();
CounterScreen screen = new CounterScreen();

String firstRender = renderer.render(screen.render());

screen.render().children().get(2).dispatch("click");
if (screen.isDirty()) {
    screen.consumeDirty();
    String secondRender = renderer.render(screen.render());
}
```

The current `VelaRenderer` output is intended for demonstration and tests. For the sample counter, it produces a tree shaped like:

```text
column {layout.arrangement=start, layout.alignment=stretch, layout.gap=0dp}
  text[Counter] {value=Counter}
  text[Current value: 1] {value=Current value: 1}
  button {state.enabled=true, semantics.role=button, label=Increment, variant=primary, enabled=true} events=[click]
```

## Build View Trees Directly

Prefer component factories for common nodes:

```java
ViewNode tree = VelaColumn.of(
    VelaText.of("Title"),
    VelaButton.of("Save")
);
```

Use `ViewNode.node(...)` for custom experimental nodes:

```java
ViewNode badge = ViewNode.node(
    "badge",
    Map.of("tone", "success", "label", "Ready"),
    List.of()
);
```

`PropKey<T>` supplies typed property access and `StandardProps` validates shared properties. The public `props()` map retains string wire values for backend compatibility. Events are stored separately as typed handlers and are exposed only through `eventNames()` and `dispatch(...)`. Children, props, and event maps are copied into unmodifiable collections when the node is created.

For richer custom nodes, use the builder:

```java
ViewNode chart = ViewNode.builder("chart")
    .key("revenue")
    .prop("kind", "bar")
    .modifier(Modifier.none().fillMaxWidth().height(280))
    .on("select", event -> openDataPoint(event.value()))
    .build();
```

## Component Lifecycle Today

When managed by `VelaRuntime`, a root is mounted, rendered, updated through a deferred scheduler, and disposed on `close()`. `onMount()` and `onUnmount()` are optional lifecycle hooks. Backend handles are retained for matching types and keys.

Manual rendering remains supported:

1. Instantiate a `VelaComponent`.
2. Call `render()` to build a fresh immutable `ViewNode` tree.
3. A renderer dispatches a node event such as `click` or `change`.
4. The event callback mutates component state through a component method.
5. Observable state calls `markDirty()` automatically; a raw annotated field requires a manual call.
6. The caller checks `isDirty()`.
7. The caller calls `consumeDirty()`.
8. The caller renders again.

`VelaComponent.onStateChanged()` is called once per clean-to-dirty transition. Existing manual integrations can continue using it; runtime-managed roots use the runtime as their scheduling owner.

## Testing Components

Unit tests can assert dirty state and rendered text:

```java
@Test
void incrementMarksTheScreenDirtyAndUpdatesTheRenderTree() {
    CounterScreen screen = new CounterScreen();

    screen.render().children().get(2).dispatch("click");

    assertTrue(screen.isDirty());
    assertTrue(new VelaRenderer().render(screen.render()).contains("Current value: 1"));
}
```

Prefer testing component behavior through public methods, dirty state, and `ViewNode` output rather than reaching into private fields.

## Commands

Run all tests:

```bash
./gradlew test
```

Run the demo app:

```bash
./gradlew :app:run
```

See [Android backend](android-backend.md) for native mounting and capabilities, and [Runtime guide](runtime-guide.md) for child scopes, saveable state, scoped cancellation, and dispatch.

## Related Pages

- [`README.md`](../README.md)
- [`components.md`](components.md)
- [`runtime-guide.md`](runtime-guide.md)
- [`project-structure.md`](project-structure.md)
- [`framework-gaps.md`](framework-gaps.md)
