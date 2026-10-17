# Vela

Latin for "sail" — giving Java devs the wind to move fast on UI without switching their entire stack.

## Introduction

Vela is a Java-first UI framework with a reusable runtime, native Android backend, concise component API, YAML resources, project CLI, and state-preserving development reload. JDK 25 is the default toolchain; shared core and Android bytecode target Java 17, with JVM compatibility tested on Java 21 and 25.

## Modules

- `vela-core` contains observable state, a mounted runtime, keyed renderer reconciliation, immutable modifiers and typography, payload-aware UI events, and a broad component catalog.
- `vela-compiler` contains optional compile-time validation for `@State`; ordinary applications need no annotation processing.
- `vela-android` contains the reusable retained Android backend, frame scheduler, image loader, and Bundle state adapter.
- `app` contains the demo application and the first real screen component, `CounterScreen`.
- `showcase-android` contains Daymark, a native Android application consuming `vela-android`.
- `vela-tooling` compiles YAML resources into typed Java accessors, binary catalogs, and Android resources.
- `vela-gradle-plugin` supplies generated application bootstrap, cached resource tasks, and reload DEX compilation.
- `vela-cli` creates projects, manages toolchains, checks prerequisites, builds, and watches edits.
- `vela-devtools-android` and `vela-dev-protocol` provide the debug-only reload connection.

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
├── vela-android/
│   └── src/main/java/dev/vela/android/
└── docs/
    ├── android-backend.md
    ├── audit-remediation.md
    ├── components.md
    ├── framework-gaps.md
    ├── usage-guide.md
    ├── runtime-guide.md
    ├── documentation-guide.md
    └── project-structure.md
```

See the [usage guide](docs/usage-guide.md) for the current public API and examples. The [component reference](docs/components.md) covers layouts, controls, modifiers, events, navigation, feedback, overlays, and data display. See the [framework gaps](docs/framework-gaps.md) for the remaining implementation backlog.

The [runtime guide](docs/runtime-guide.md) documents observable state, deferred updates, lifecycle hooks, and retained backend handles.

## Requirements

- The CLI runtime archive needs no separately installed Java or Gradle. `vela setup` manages JDK 25 and the Android SDK.
- Framework contributors: JDK 25 by default, or JDK 21 with `-PvelaJavaVersion=21`
- A shell that can run the Gradle wrapper
- Android SDK 35 for the optional Daymark showcase

## Create an application

Build the CLI archive with `./gradlew -Pandroid=true :vela-cli:runtimeDist`, unpack the archive from `vela-cli/build/distributions/`, and add its `vela/bin` directory to PATH.

```sh
vela init my-app --package com.example.myapp
cd my-app
vela setup
vela doctor
vela dev
```

The starter uses one Java screen and YAML values. Its manifest, theme, Activity lifecycle, state restoration, and build setup are supplied automatically. `vela setup --emulator` installs an optional emulator; Android Studio is optional. See the [developer workflow](docs/developer-workflow.md) for requirements, resource syntax, concise APIs, managed installation, reload boundaries, and testing.

## Getting Started

Run the tests to verify the workspace:

```bash
./gradlew test
```

Android modules are included automatically when `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to an SDK directory. Without an SDK, the default test command verifies the JVM modules. Use `-Pandroid=false` for an explicitly JVM-only build, or `-Pandroid=true` to enable Android modules (including SDK configuration through `local.properties`).

Run the demo app:

```bash
./gradlew :app:run
```

Build the Android showcase:

```bash
./gradlew -Pandroid=true :showcase-android:assembleDebug --no-configuration-cache
```

See [`showcase-android/README.md`](showcase-android/README.md) for device installation and launch commands.

The demo prints the rendered view tree for `CounterScreen`, increments the counter, and renders again so you can see the state change.

## How the Pieces Fit Together

`CounterScreen` extends `VelaComponent` and owns a `MutableState<Integer>`. Changed values automatically mark it dirty. `VelaRuntime` batches root updates and reconciles its tree through `TextRendererBackend`, retaining matching handles and refreshing event callbacks. Legacy `@State` validation and direct `VelaRenderer` output remain supported.

The annotation processor in `vela-compiler` is intentionally small for now. Its job is to keep state usage honest and provide a dedicated place for compile-time generation later if you want to expand the framework.

## Documentation

| Page | Purpose |
| --- | --- |
| [`docs/android-backend.md`](docs/android-backend.md) | Native backend capabilities, mounting, lifecycle, image loading, and device tests |
| [`docs/audit-remediation.md`](docs/audit-remediation.md) | Audit fixes, regression coverage, and remaining boundaries |
| [`docs/usage-guide.md`](docs/usage-guide.md) | How to build components, use state, create view trees, render output, and test behavior |
| [`docs/runtime-guide.md`](docs/runtime-guide.md) | Observable state, mounted runtime, scheduling, lifecycle, identity, and renderer contracts |
| [`docs/components.md`](docs/components.md) | Complete component catalog, modifiers, events, semantics, and renderer contract |
| [`docs/project-structure.md`](docs/project-structure.md) | Module ownership, package map, dependency flow, and runtime flow |
| [`docs/framework-gaps.md`](docs/framework-gaps.md) | Missing framework capabilities and a suggested implementation order |
| [`docs/documentation-guide.md`](docs/documentation-guide.md) | Documentation format, ownership, and maintenance workflow |

## Verification

The current workspace is validated with:

```bash
./gradlew test
```
