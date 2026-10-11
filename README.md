# Vela

Latin for "sail" — giving Java devs the wind to move fast on UI without switching their entire stack.

## Introduction

Vela is a Java 21 workspace with a reusable UI runtime, annotation processor, console demo, and installable Android showcase.

## Modules

- `vela-core` contains the shared runtime model, immutable modifiers and typography, payload-aware UI events, and a broad declarative component catalog.
- `vela-compiler` contains the annotation processor used to validate `@State` usage during compilation.
- `app` contains the demo application and the first real screen component, `CounterScreen`.
- `showcase-android` contains Daymark, a polished native Android application and Vela-to-Android renderer.

## Project Structure

```text
Vela/
├── app/
│   └── src/main/java/dev/vela/app/
│       ├── App.java
│       └── CounterScreen.java
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
    ├── framework-gaps.md
    ├── usage-guide.md
    ├── documentation-guide.md
    └── project-structure.md
```

See the [usage guide](docs/usage-guide.md) for the current public API and examples. The [component reference](docs/components.md) covers layouts, controls, modifiers, events, navigation, feedback, overlays, and data display. See the [framework gaps](docs/framework-gaps.md) for the remaining implementation backlog.

## Requirements

- Java 21 or newer
- A shell that can run the Gradle wrapper
- Android SDK 35 for the optional Daymark showcase

## Getting Started

Run the tests to verify the workspace:

```bash
./gradlew test
```

Run the demo app:

```bash
./gradlew :app:run
```

Build the Android showcase:

```bash
./gradlew :showcase-android:assembleDebug --no-configuration-cache
```

See [`showcase-android/README.md`](showcase-android/README.md) for device installation and launch commands.

The demo prints the rendered view tree for `CounterScreen`, increments the counter, and renders again so you can see the state change.

## How the Pieces Fit Together

`CounterScreen` extends `VelaComponent` and uses `@State` for mutable state. When the state changes, `StateManager` marks the component dirty, and `VelaRenderer` can render the current `ViewNode` tree.

The annotation processor in `vela-compiler` is intentionally small for now. Its job is to keep state usage honest and provide a dedicated place for compile-time generation later if you want to expand the framework.

## Documentation

| Page | Purpose |
| --- | --- |
| [`docs/usage-guide.md`](docs/usage-guide.md) | How to build components, use state, create view trees, render output, and test behavior |
| [`docs/components.md`](docs/components.md) | Complete component catalog, modifiers, events, semantics, and renderer contract |
| [`docs/project-structure.md`](docs/project-structure.md) | Module ownership, package map, dependency flow, and runtime flow |
| [`docs/framework-gaps.md`](docs/framework-gaps.md) | Missing framework capabilities and a suggested implementation order |
| [`docs/documentation-guide.md`](docs/documentation-guide.md) | Documentation format, ownership, and maintenance workflow |

## Verification

The current workspace is validated with:

```bash
./gradlew test
```
