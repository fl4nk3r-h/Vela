# Project Structure

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Contributors and coding agents |
| Owner | Vela maintainers |
| Scope | Gradle modules and runtime flow |

## Overview

Vela is a Java 21 Gradle workspace split into three modules. The split keeps reusable runtime code, compile-time validation, and the executable example independent while allowing the `app` module to exercise the complete flow.

## Module Ownership

| Module | Responsibility | Main dependency relationship |
| --- | --- | --- |
| `vela-core` | Runtime component model, immutable view tree, state dirtiness, and text rendering | No project dependency |
| `vela-compiler` | Annotation processor that validates `@State` fields | Depends on `vela-core` |
| `app` | Runnable counter example and integration tests | Depends on `vela-core`; applies `vela-compiler` as an annotation processor |

Keep shared framework behavior in `vela-core`. Keep compile-time checks in `vela-compiler`. Keep product or demo screens in `app`.

## Package Map

### `vela-core`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.annotation` | `State` | Marks mutable fields that participate in component state |
| `dev.vela.components` | `VelaText`, `VelaButton`, `VelaColumn` | Small factories for common `ViewNode` shapes |
| `dev.vela.core` | `VelaComponent`, `StateManager`, `ViewNode`, `VelaRenderer` | Component lifecycle, dirty state, view-tree data, and text output |

### `vela-compiler`

| Package | Key type | Responsibility |
| --- | --- | --- |
| `dev.vela.processor` | `StateProcessor` | Rejects `@State` usage outside fields and on final fields |

### `app`

| Package | Key types | Responsibility |
| --- | --- | --- |
| `dev.vela.app` | `App`, `CounterScreen` | Demonstrates state mutation, dirty tracking, and rendering |

## Dependency Flow

```text
app
├── runtime dependency ──> vela-core
└── annotation processor ─> vela-compiler ──> vela-core
```

The compiler is active during `app` compilation. It does not replace runtime state management; it validates source-level use of `@State` before the application runs.

## Runtime Flow

1. `App` creates a `CounterScreen` and renders its `ViewNode` tree.
2. `CounterScreen.increment()` updates its `@State` field and calls `markDirty()`.
3. `StateManager` records the dirty flag and invokes the component callback once per clean-to-dirty transition.
4. `App` checks `isDirty()`, consumes the flag, and renders the updated tree through `VelaRenderer`.
5. `VelaRenderer` walks the immutable tree and produces an indented text representation.

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
│       └── core/
├── vela-compiler/
│   └── src/main/java/dev/vela/processor/StateProcessor.java
└── docs/
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

The demo logs the initial counter tree, increments the counter, consumes the dirty flag, and logs the updated tree.

## Related Pages

- [`README.md`](../README.md)
- [`documentation-guide.md`](documentation-guide.md)
- [`framework-gaps.md`](framework-gaps.md)
- [`usage-guide.md`](usage-guide.md)
- [`agents.md`](../agents.md)
