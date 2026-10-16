# Daymark Android Showcase

Daymark is an installable Android application built from Vela `ViewNode` trees. It demonstrates a native renderer, state-driven rerendering, event dispatch, modifiers, responsive layouts, and a broad selection of Vela components.

## Features

- Three interactive pages: Today, Insights, and Profile
- Focus sessions, hydration, tasks, energy, notification settings, and weekly pacing
- Vela layouts, cards, buttons, chips, badges, progress, tables, switches, sliders, lists, navigation, and alerts
- Native Android rendering without AndroidX or an external UI toolkit
- No network permission or personal-data collection

## Requirements

- Android SDK 35
- Android device running API 26 or newer
- USB debugging enabled for command-line installation

## Build

```bash
./gradlew -Pandroid=true :showcase-android:assembleDebug --no-configuration-cache
```

The APK is written to:

```text
showcase-android/build/outputs/apk/debug/showcase-android-debug.apk
```

## Install And Launch

List connected devices:

```bash
adb devices
```

Install the debug build:

```bash
adb install -r showcase-android/build/outputs/apk/debug/showcase-android-debug.apk
```

Launch Daymark:

```bash
adb shell am start -n dev.vela.showcase/.MainActivity
```

## Architecture

`DaymarkScreen` owns saveable observable state. `MainActivity` mounts it through `VelaRuntime`, `VelaAndroidBackend`, and `AndroidFrameScheduler`. Native views are retained and receive current descriptions and callbacks. Activity recreation restores component values and native scroll/focus/selection state. Teardown cancels scheduled work and releases native handles.

The shared Android implementation lives in `vela-android`. See [backend capabilities and tests](../docs/android-backend.md) and the [audit remediation record](../docs/audit-remediation.md).

```sh
./gradlew -Pandroid=true :vela-android:connectedDebugAndroidTest :showcase-android:connectedDebugAndroidTest --no-configuration-cache
```
