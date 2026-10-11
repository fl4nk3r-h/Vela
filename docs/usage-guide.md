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

The current renderer is text-based. Vela does not yet include a native desktop, Android, web, or terminal UI backend. Treat the existing implementation as the framework kernel: component shape, state marking, immutable nodes, simple factories, and compile-time `@State` validation.

## Mental Model

| Compose-like concept | Vela equivalent today | Current behavior |
| --- | --- | --- |
| Composable function or component | `VelaComponent` subclass | Owns state and returns a `ViewNode` from `render()` |
| Mutable state | Field annotated with `@State` | Compile-time validated, but changes must call `markDirty()` manually |
| Composition tree | `ViewNode` | Immutable node with type, optional text, string props, and children |
| Recomposition request | Dirty flag in `StateManager` | Set by `markDirty()` and consumed by the caller |
| UI element factories | `VelaText`, `VelaButton`, `VelaColumn` | Convenience factories that create `ViewNode` instances |
| Renderer | `VelaRenderer` | Produces an indented text representation of a tree |

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

`render()` should describe the current UI from current component state. It should avoid mutating state directly; mutate state from explicit component methods and then mark the component dirty.

## Add State

Annotate mutable state fields with `@State`:

```java
import dev.vela.annotation.State;

public final class CounterScreen extends VelaComponent {
    @State
    private int count;

    public void increment() {
        count++;
        markDirty();
    }

    @Override
    public ViewNode render() {
        return VelaColumn.of(
            VelaText.of("Counter"),
            VelaText.of("Current value: " + count)
        );
    }
}
```

Important current constraints:

| Rule | Reason |
| --- | --- |
| `@State` can only be used on fields | Enforced by `StateProcessor` |
| `@State` fields cannot be `final` | Enforced by `StateProcessor` because state must be mutable |
| `@State` fields must belong to a `VelaComponent` subclass | Prevents state annotations on objects that cannot participate in invalidation |
| State changes must call `markDirty()` manually | No generated setter or observable state wrapper exists yet |
| The dirty flag is component-local | There is no runtime tree scheduler yet |

## Render A Component

Use `VelaRenderer` to render the component's current tree:

```java
VelaRenderer renderer = new VelaRenderer();
CounterScreen screen = new CounterScreen();

String firstRender = renderer.render(screen.render());

screen.increment();
if (screen.isDirty()) {
    screen.consumeDirty();
    String secondRender = renderer.render(screen.render());
}
```

The current `VelaRenderer` output is intended for demonstration and tests. For the sample counter, it produces a tree shaped like:

```text
column
  text[Counter] {value=Counter}
  text[Current value: 1] {value=Current value: 1}
  button {label=Increment}
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

All props are currently `String` values. Children and props are copied into unmodifiable collections when the node is created.

## Component Lifecycle Today

The current lifecycle is intentionally simple:

1. Instantiate a `VelaComponent`.
2. Call `render()` to build a fresh immutable `ViewNode` tree.
3. Mutate component state through a component method.
4. The method calls `markDirty()`.
5. The caller checks `isDirty()`.
6. The caller calls `consumeDirty()`.
7. The caller renders again.

`VelaComponent.onStateChanged()` is a protected hook. It is called once when the component moves from clean to dirty. Integrations can override it later to schedule a render.

## Testing Components

Unit tests can assert dirty state and rendered text:

```java
@Test
void incrementMarksTheScreenDirtyAndUpdatesTheRenderTree() {
    CounterScreen screen = new CounterScreen();

    screen.increment();

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

## Related Pages

- [`README.md`](../README.md)
- [`project-structure.md`](project-structure.md)
- [`framework-gaps.md`](framework-gaps.md)
