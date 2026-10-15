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
| `vela-android` | Retained native widgets, frame scheduling, image loading, lazy adapters, and Bundle state | Depends on `vela-core` |
| `showcase-android` | Native Daymark application and Activity integration | Depends on `vela-android`; applies `vela-compiler` as an annotation processor |

Keep shared framework behavior in `vela-core`. Keep compile-time checks in `vela-compiler`. Keep console examples in `app`, reusable Android behavior in `vela-android`, and showcase-only screens in `showcase-android`. Android modules are optional; `-Pandroid=false` excludes them, while `-Pandroid=true` enables them.

## Package Map

### `vela-core`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.annotation` | `State` | Marks mutable fields that participate in component state |
| `dev.vela.components` | Layouts, controls, navigation, feedback, overlays, and data display | Typed factories for semantic `ViewNode` shapes |
| `dev.vela.core` | `VelaComponent`, `MutableState`, `VelaRuntime`, `MountedTree`, `RendererBackend`, `ViewNode`, `UiEvent`, `VelaRenderer` | State, mounting, scheduling, reconciliation, view data, events, and text output |
| `dev.vela.ui` | `Modifier`, `TextStyle`, alignment, arrangement, orientation, variants | Shared immutable UI configuration |

### `vela-compiler`

| Package | Key type | Responsibility |
| --- | --- | --- |
| `dev.vela.processor` | `StateProcessor` | Rejects invalid, final, or non-`VelaComponent` state fields |

### `app`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.app` | `App`, `CounterScreen` | Demonstrates state mutation, dirty tracking, and rendering |

### `vela-android`

`dev.vela.android` contains `VelaAndroidBackend`, `AndroidFrameScheduler`, `AndroidState`, and `ImageLoader`. Instrumented tests live in `src/androidTest`.

### `showcase-android`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.showcase` | `DaymarkScreen`, `MainActivity` | Builds a Vela tree, mounts the shared backend, and saves/restores Activity state |

## Dependency Flow

```text
app
├── runtime dependency ──> vela-core
└── annotation processor ─> vela-compiler ──> vela-core

showcase-android
├── runtime dependency ──> vela-android ──> vela-core
└── annotation processor ─> vela-compiler ──> vela-core
```

The compiler is active during `app` compilation. It does not replace runtime state management; it validates source-level use of `@State` before the application runs.

Daymark follows the same retained runtime flow through `VelaAndroidBackend` and `AndroidFrameScheduler`. Its saveable observable state is restored before mounting; native view state is restored afterward. The Activity closes the runtime, backend, and scheduler on destruction.

## Runtime Flow

1. `App` mounts `CounterScreen` through `VelaRuntime` and `TextRendererBackend`.
2. A retained button handle dispatches `click` through its current `ViewNode` description.
3. `CounterScreen.increment()` updates observable state, automatically marking the owner dirty.
4. The runtime queues one deferred callback while invalidations are pending.
5. The callback renders the latest state and reconciles matching type/key handles through `MountedTree`.
6. The text backend exposes the current diagnostic output; closing the runtime releases the mounted tree.

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
├── vela-android/
│   └── src/main/java/dev/vela/android/
├── showcase-android/
│   └── src/main/java/dev/vela/showcase/
└── docs/
    ├── components.md
    ├── documentation-guide.md
    ├── framework-gaps.md
    ├── project-structure.md
    ├── runtime-guide.md
    └── usage-guide.md
```

## Verification Commands

Run local JVM tests (and Android unit test tasks when Android modules are enabled):

```bash
./gradlew test
```

Run the executable demonstration:

```bash
./gradlew :app:run
```

Build the Android showcase:

```bash
./gradlew -Pandroid=true :showcase-android:assembleDebug --no-configuration-cache
```

The demo logs the initial counter tree, increments the counter, drains its scheduled frame, and logs the updated tree. Device tests and native capabilities are described in [Android backend](android-backend.md).

## Related Pages

- [`README.md`](../README.md)
- [`documentation-guide.md`](documentation-guide.md)
- [`components.md`](components.md)
- [`framework-gaps.md`](framework-gaps.md)
- [`usage-guide.md`](usage-guide.md)
- [`runtime-guide.md`](runtime-guide.md)
- [`agents.md`](../agents.md)
