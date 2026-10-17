# Developer Workflow

This page describes creating, configuring, building, and reloading Android applications with Vela.

## Page metadata

| Field | Value |
| --- | --- |
| Status | Implemented; development reload is a developer preview |
| Audience | Application developers and framework contributors |
| Owner | Vela maintainers |
| Scope | Java compatibility, concise APIs, YAML resources, CLI setup, and Android reload |

## Requirements and installation

The CLI runtime archive includes Java for running the CLI. A separately installed Java or Gradle is unnecessary for starting a new project. Setup provisions a full JDK 25 for compilation and the Android SDK. Android Studio is optional.

| Requirement | Purpose |
| --- | --- |
| Linux x64, macOS x64/ARM64, or Windows x64 | Supported managed Android toolchain hosts |
| Initial internet access and writable storage | Gradle, JDK, Android SDK, dependencies, and optional emulator downloads |
| A terminal and ZIP extraction tool | Launch the unpacked CLI |
| Android API 26+ device with USB debugging, or an emulator | Run and reload the application |
| Hardware virtualization for an emulator | Run Android virtual devices efficiently |

JDK archives are pinned Eclipse Temurin 25 downloads. Android command-line tools are pinned to version 22.0. Their official SHA-256 checksums are bundled in the CLI. SDK package management installs platform 35 and build tools 35.0.0 by default. Managed Android installation on Linux ARM64 is unavailable; an existing compatible SDK can be supplied.

Unpack the platform's `vela-0.1.0-<os>-<arch>.zip`, then put its `vela/bin` directory on your PATH. Windows uses `vela.bat`. The archive's runtime is selected even when `JAVA_HOME` points elsewhere.

To build that archive from this repository with an Android SDK available:

```sh
./gradlew -Pandroid=true :vela-cli:runtimeDist
```

The archive is written to `vela-cli/build/distributions/`. It includes a local Maven repository containing matching framework, compiler, development, and Gradle plugin artifacts. No public Maven publication is required to try a generated project.

## Create and initialize an application

```sh
vela init my-app --package com.example.myapp
cd my-app
vela setup
vela doctor
vela dev
```

Initialization refuses to overwrite a nonempty directory. It creates:

```text
my-app/
  vela.yaml
  settings.gradle
  build.gradle
  gradle.properties
  gradlew / gradlew.bat
  gradle/wrapper/
  src/main/java/com/example/myapp/MainScreen.java
  src/main/vela/values.yaml
  .gitignore
  README.md
```

Setup reuses a complete JDK 25 and SDK from the environment when available. Otherwise it installs them in `~/.vela`; override that location with `VELA_CACHE`. It records local paths under `.vela/`, writes Android's `local.properties`, and copies the bundled framework repository into `.vela/repository`. These machine-specific files are ignored by Git. After cloning a generated application, run `vela setup` again.

SDK licenses are reviewed interactively. `--accept-licenses` supplies acceptance explicitly for unattended installation after reviewing the terms. Downloads use partial files, checksum verification, archive path validation, and completed-installation markers. A failed download does not become an offline cache hit.

```sh
vela setup --emulator
```

This also installs a Google APIs API 35 emulator image and creates `vela-api-35`. Start it with Android Studio or the printed SDK's `emulator/emulator -avd vela-api-35` executable. Physical devices may need operating-system USB drivers or permissions.

| Command | Behavior |
| --- | --- |
| `vela doctor` | Check compiler, platform, build tools, ADB, wrapper, framework artifacts, and connected devices |
| `vela dev [--device SERIAL]` | Initial debug installation followed by watched reloads |
| `vela run [--device SERIAL]` | Build, install, and launch debug |
| `vela test` | JVM unit tests and Android debug lint |
| `vela test --connected` | Also run Android instrumentation tests |
| `vela build` | Build release; configure application signing before distribution |
| `vela dev --once` | Exit after processing one watched edit; useful for automation |

Select a device when several are connected. `ANDROID_SERIAL` is also supported. Ordinary `./gradlew` commands and IDE builds remain available after setup; select the JDK path printed by setup as the IDE's Gradle JDK.

## Write less Java

The facade delegates to the existing validated component factories. All 75 factory overloads have concise equivalents. Existing `VelaButton`, `VelaColumn`, and other component APIs continue to work.

```java
import static dev.vela.ui.UI.*;
import dev.vela.core.*;

public final class Counter extends VelaComponent {
    private final MutableState<Integer> count = state("count", 0);

    @Override public ViewNode render() {
        return column(
            text("Count: " + count.get()),
            button("Increment", () -> count.update(n -> n + 1))
        ).with(gap(12).padding(16));
    }
}
```

`state(key, initialValue)` supports String, int, long, double, and boolean. Keys are explicit and unique within a component. The one-argument `state(initialValue)` remains temporary. Use `saveableState(key, initialValue, codec)` for custom data, and its four-argument overload to supply a stable format/version identifier when a custom encoding can change.

Bindings eliminate setter callbacks:

```java
field("Name", name);
toggle("Notifications", enabled);
checkbox("Accept", accepted);
slider("Volume", volume, SliderOptions.defaults().range(0, 10).step(0.5));
```

`ButtonOptions`, `FieldOptions`, and `SliderOptions` supply immutable named options. `ViewNode.with(modifier)` preserves callbacks, keys, children, and lazy content while returning a modified copy. Existing factory options still determine control structure and event availability; changing a disabled factory option requires constructing the next description with the desired option.

`@State` processing is optional validation. It does not rewrite assignments or generate setters, and generated applications do not enable it. Screen code uses ordinary incremental Java compilation.

## Replace resource XML with YAML

Put shared values in `src/main/vela/values.yaml`:

```yaml
strings:
  title: 'My application'
  count: '{0,number,integer} items'
colors:
  primary: '#176B4B'
dimensions:
  spacing: 16dp
  title_size: 24sp
styles:
  page:
    padding: '@dimensions.spacing'
    gap: '@dimensions.spacing'
  heading:
    color: '@colors.primary'
    fontSize: '@dimensions.title_size'
themes:
  default:
    background: '#F4F6F2'
```

Read generated typed accessors inside a render pass:

```java
var values = Values.of(resources());
return column(
    text(values.strings().title()).with(values.styles().heading()),
    text(values.strings().count(3))
).with(values.styles().page().then(values.themes().defaultValue()));
```

Names use lowercase letters, numbers, and underscores. Java keywords receive a `Value` suffix in accessors, such as `defaultValue()`. Colors use quoted `#RRGGBB` or `#AARRGGBB`; dimensions use nonnegative integer `dp` or `sp`. Named styles/themes produce modifiers and are applied explicitly. The generated platform theme supplies an Android launch window; YAML themes style the Vela tree.

Add `values-fr.yaml`, or another BCP 47 locale such as `values-pt-BR.yaml`, containing translated `strings`. Missing translations fall back to the base file. Runtime resolution uses the hosting Activity's locale with language/script/exact-tag fallback. Parameters use Java `MessageFormat` syntax, consecutive argument numbers starting at zero, and matching argument types across translations. Format patterns follow Java apostrophe escaping rules.

The build rejects duplicate keys, missing references, cycles, invalid values, unsupported properties, and inconsistent parameters with source locations. YAML is parsed only by tooling. Android applications contain a compact binary catalog and generated Java getters. Generated `vela_` Android string/color/dimension resources are available for platform integration; formatted strings should be consumed through `Values`.

`generateVelaResources` is cacheable. Unchanged inputs skip generation. Value-only edits preserve the generated Java file's content and modification time; only API-shape changes regenerate it. `generateVelaBootstrap` creates the manifest, platform theme, and entry-class keep rule. Advanced applications can override Android source sets and signing through ordinary Gradle configuration.

## Reload behavior and boundaries

`vela dev` keeps Gradle's daemon and caches available. Source edits use Javac outputs and SDK D8 to produce an application DEX payload. The debug dependency loads application classes in a fresh namespace-isolated loader while sharing Android, framework, and dependency classes. It does not redefine fields on existing objects.

The replacement screen restores compatible keyed state and renders in a separate surface before becoming visible. Rejected compilation or replacement leaves the previous working screen active. Committed replacements dispose previous component scopes and native handles. Register background work with `onDispose` so it is canceled with its owning scope.

| Edit | Action |
| --- | --- |
| Java methods, fields, or components in the configured application package | Compile and replace the screen without APK assembly or installation |
| YAML value with unchanged resource API | Transfer catalog and render again without Java compilation |
| YAML resource keys/types/arguments | Regenerate accessors and perform code reload |
| Manifest/configuration, dependencies, assets, native resources/libraries, or code outside the reloadable package | Build and reinstall, attempting a state checkpoint first |
| Payload requiring multiple DEX files or exceeding the transfer limit | Full restart fallback |

Use matching source directories under `src/main/java/<application-package>/` for reloadable code. APK resource-ID references remain tied to the installed APK; edits to native platform resources need a restart. Application statics and arbitrary object graphs are not transferred. New, removed, or incompatible state keys use defaults; custom encodings should declare a version identifier. Native focus, selection, scroll state, and keyed child scopes are restored where matching controls remain.

The latest successful development code and catalog survive Activity recreation within the process. A full process restart starts installed code; a development restart checkpoints compatible state in private application storage. Changing application ID cannot transfer that private data to a different application.

The bridge uses a per-session token, bounded protocol messages, and an ADB-forwarded local socket. It requires no network permission. The bridge, protocol implementation, and in-memory DEX loader dependency are absent from release APKs.

## Compatibility and verification

JDK 25 is the default development toolchain. Shared core and Android classes target Java 17 bytecode. This does not make the full Java 25 standard library available on Android. Keep Android application language/API usage within the SDK and desugaring support. JVM verification also runs with `-PvelaJavaVersion=21`.

```sh
./gradlew -Pandroid=false test :app:run
./gradlew -Pandroid=false -PvelaJavaVersion=21 test :app:run
./gradlew -Pandroid=true :vela-android:connectedDebugAndroidTest :showcase-android:connectedDebugAndroidTest
python3 tools/verify_dev_workflow.py --cli vela-cli/build/install/vela/bin/vela --work-dir /tmp/vela-workflow --device emulator-5554
```

The workflow verifier creates a disposable application, edits YAML and Java, exercises state and child scopes, tests failed compilation/rendering and restart recovery, checks unchanged application PID/APK timestamps, and inspects release DEX files. It records measured edit timings in its work directory. Timings depend on hardware, project size, emulator, and warm caches; Vela does not promise a universal reload latency.

Related: [README](../README.md), [runtime guide](runtime-guide.md), [Android backend](android-backend.md), [framework gaps](framework-gaps.md).
