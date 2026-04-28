# Vela

Latin for "sail" — giving Java devs the wind to move fast on UI without switching their entire stack.

## Introduction

Vela is a small Java 21 workspace split into three modules: a reusable core runtime, an annotation processor, and a demo app that shows how a screen component can be built and rendered.

## Modules

- `vela-core` contains the shared runtime model: `VelaComponent`, `ViewNode`, `VelaRenderer`, `StateManager`, the `@State` annotation, and simple component factories.
- `vela-compiler` contains the annotation processor used to validate `@State` usage during compilation.
- `app` contains the demo application and the first real screen component, `CounterScreen`.

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
│       └── core/
├── vela-compiler/
│   └── src/main/java/dev/vela/processor/StateProcessor.java
└── docs/
    └── project-structure.md
```

## Requirements

- Java 21 or newer
- A shell that can run the Gradle wrapper

## Getting Started

Run the tests to verify the workspace:

```bash
./gradlew test
```

Run the demo app:

```bash
./gradlew :app:run
```

The demo prints the rendered view tree for `CounterScreen`, increments the counter, and renders again so you can see the state change.

## How the Pieces Fit Together

`CounterScreen` extends `VelaComponent` and uses `@State` for mutable state. When the state changes, `StateManager` marks the component dirty, and `VelaRenderer` can render the current `ViewNode` tree.

The annotation processor in `vela-compiler` is intentionally small for now. Its job is to keep state usage honest and provide a dedicated place for compile-time generation later if you want to expand the framework.

## Verification

The current workspace is validated with:

```bash
./gradlew test
```
