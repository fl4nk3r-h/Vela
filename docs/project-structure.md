# Project Structure

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Contributors and coding agents |
| Owner | Vela maintainers |
| Scope | Gradle modules and runtime flow |

## Overview

Vela is a Java 21 Gradle workspace split into reusable runtime, compiler, console demo, and Android showcase modules.

## Module Ownership

| Module | Responsibility | Main dependency relationship |
| --- | --- | --- |
| `vela-core` | Runtime component model, immutable view tree, UI catalog, modifiers, events, state dirtiness, and text rendering | No project dependency |
| `vela-compiler` | Annotation processor that validates `@State` fields | Depends on `vela-core` |
| `app` | Runnable counter example and integration tests | Depends on `vela-core`; applies `vela-compiler` as an annotation processor |
| `showcase-android` | Native Daymark application and Android `View` renderer | Depends on `vela-core`; applies `vela-compiler` as an annotation processor |

Keep shared framework behavior in `vela-core`. Keep compile-time checks in `vela-compiler`. Keep console examples in `app` and Android-specific rendering in `showcase-android` until a dedicated backend module is introduced.

## Package Map

### `vela-core`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.annotation` | `State` | Marks mutable fields that participate in component state |
| `dev.vela.components` | Layouts, controls, navigation, feedback, overlays, and data display | Typed factories for semantic `ViewNode` shapes |
| `dev.vela.core` | `VelaComponent`, `StateManager`, `ViewNode`, `UiEvent`, `VelaRenderer` | Component lifecycle, dirty state, view-tree data, event dispatch, and text output |
| `dev.vela.ui` | `Modifier`, `TextStyle`, alignment, arrangement, orientation, variants | Shared immutable UI configuration |

### `vela-compiler`

| Package | Key type | Responsibility |
| --- | --- | --- |
| `dev.vela.processor` | `StateProcessor` | Rejects invalid, final, or non-`VelaComponent` state fields |

### `app`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.app` | `App`, `CounterScreen` | Demonstrates state mutation, dirty tracking, and rendering |

### `showcase-android`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.showcase` | `DaymarkScreen`, `VelaAndroidRenderer`, `MainActivity` | Builds a rich Vela tree, maps it to native Android views, and schedules rerenders |

## Dependency Flow

```text
app
├── runtime dependency ──> vela-core
└── annotation processor ─> vela-compiler ──> vela-core

showcase-android
├── runtime dependency ──> vela-core
└── annotation processor ─> vela-compiler ──> vela-core
```

The compiler is active during `app` compilation. It does not replace runtime state management; it validates source-level use of `@State` before the application runs.

Daymark follows the same state flow, but `VelaAndroidRenderer` maps the tree to native views. Native callbacks dispatch node events, `MainActivity` consumes the dirty flag, and the screen is rendered again.

## Runtime Flow

1. `App` creates a `CounterScreen` and renders its `ViewNode` tree.
2. A renderer or test dispatches the button node's `click` event.
3. `CounterScreen.increment()` updates its `@State` field and calls `markDirty()`.
4. `StateManager` records the dirty flag and invokes the component callback once per clean-to-dirty transition.
5. `App` checks `isDirty()`, consumes the flag, and renders the updated tree through `VelaRenderer`.
6. `VelaRenderer` walks the immutable tree and produces an indented text representation.

## Source Layout

```text
Vela/
├── app/
│   ├── src/main/java/dev/vela/app/
│   │   ├── App.java
│   │   └── CounterScreen.java
│   └── src/test/java/dev/vela/app/
│       └── CounterScreenTest.java
├── vela-core/
│   └── src/main/java/dev/vela/
│       ├── annotation/State.java
│       ├── components/
│       ├── core/
│       └── ui/
├── vela-compiler/
│   └── src/main/java/dev/vela/processor/StateProcessor.java
├── showcase-android/
│   └── src/main/java/dev/vela/showcase/
└── docs/
    ├── components.md
    ├── documentation-guide.md
    ├── framework-gaps.md
    ├── project-structure.md
    └── usage-guide.md
```

## Verification Commands

Run the complete test suite:

```bash
./gradlew test
```

Run the executable demonstration:

```bash
./gradlew :app:run
```

Build the Android showcase:

```bash
./gradlew :showcase-android:assembleDebug --no-configuration-cache
```

The demo logs the initial counter tree, increments the counter, consumes the dirty flag, and logs the updated tree.

## Related Pages

- [`README.md`](../README.md)
- [`documentation-guide.md`](documentation-guide.md)
- [`components.md`](components.md)
- [`framework-gaps.md`](framework-gaps.md)
- [`usage-guide.md`](usage-guide.md)
- [`agents.md`](../agents.md)
