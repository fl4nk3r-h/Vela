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
./gradlew :showcase-android:assembleDebug --no-configuration-cache
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

`DaymarkScreen` is a `VelaComponent`. Its `render()` method creates the complete application tree from Vela components. Native view callbacks call `ViewNode.dispatch(...)`; state mutations mark the component dirty; `MainActivity` schedules a fresh render; and `VelaAndroidRenderer` maps the resulting tree to Android `View` objects.

The renderer is intentionally local to this showcase while the framework's shared renderer interface and mounted runtime are still being designed.
