# Android Backend

Use this page to mount Vela trees on native Android widgets and understand backend capabilities.

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Experimental, implemented |
| Audience | Application developers and backend maintainers |
| Owner | Vela maintainers |
| Scope | `vela-android`, native capabilities, lifecycle, images, lazy collections, and verification |

## Mount And Close

`vela-android` is an Android library depending on `vela-core`. `showcase-android` consumes it. Create the backend, scheduler, and runtime on the main thread:

```java
AndroidFrameScheduler scheduler = new AndroidFrameScheduler();
VelaAndroidBackend backend = new VelaAndroidBackend(activity, hostViewGroup);
VelaRuntime<VelaAndroidBackend.Handle> runtime = new VelaRuntime<>(screen, backend, scheduler);
runtime.mount();

// In Activity.onDestroy(), close in this order, using finally for cleanup:
runtime.close();
backend.close();
scheduler.close();
```

Each handle exposes `view()` for its modifier boundary and `widget()` for its native widget. Updates retain both unless a structural widget option changes, such as scroll orientation or avatar image/initials mode. New descriptions refresh callbacks. Identical cached descriptions skip reconciliation. The scheduler posts to Android's frame loop, accepts background dispatch, and removes queued work on close.

## Capability Matrix

| Catalog area | Native behavior |
| --- | --- |
| Rows, columns, grids, boxes, cards, surfaces, scaffolds, slots | Retained native layouts; ordered managed children; named scaffold content weight |
| Text, badges, icons, avatars, dividers, skeletons | Native text, drawables, and placeholders; named icons use a small glyph set |
| Buttons, chips, links, tabs, breadcrumbs, menus, navigation | Native actions bound to current node callbacks; removable chips have a remove action |
| Text fields | Editable text, input type, multiline, placeholder, selection retention, and form submission |
| Checkbox, radio, switch | Native checked state; touch, keyboard, and accessibility activation |
| Slider | Native step indices using `SliderRange`; the maximum is always selectable |
| Select, segmented control, pagination | Native selection and validated change events |
| Date picker | Native `DatePickerDialog` with date bounds and selected-date callback |
| Dialog, drawer | Native modal windows, dismissal, and drawer edge placement |
| Accordion | Expansion callback and conditional content visibility |
| Form, alert, snackbar, code block | Explicit submit, dismiss, action, or copy control where the node registers that event |
| Tooltip | Android tooltip text on a retained anchor boundary |
| List, table, key/value data | Eager native layouts; use lazy components for large collections |
| Lazy list, lazy grid | Native `ListView`/`GridView` adapters materializing visible and recycled rows |
| Images | Asynchronous resources, assets, content/file URIs, and HTTPS; scale applied; stale loads cancelled |

`supportedTypes()` exposes all recognized node types. Unknown types and unknown namespaced standard properties throw an explicit diagnostic. Custom chart/media widgets require another backend or extending the Android adapter; they do not silently become generic panels.

The snackbar is an application-controlled inline status/action view, not an automatically timed global notification. Icons use the documented small glyph mapping with a generic glyph for other names. Styling is native and experimental; these components are not a Material Design conformance claim.

## Layout And Semantics

The adapter supports every public `Modifier` property: width/height and min/max bounds, aspect ratio when one axis is constrained, padding/margin, colors, border/corner radius, elevation/opacity, weight, cross-axis alignment, z-index, visibility/enabled state, focusability, scroll wrapping, descriptions, roles, and test tags. Borders do not require a background color.

`SPACE_BETWEEN`, `SPACE_AROUND`, and `SPACE_EVENLY` distribute remaining space. `STRETCH` supplies cross-axis fill. Explicit width and height take priority when both axes are constrained; max bounds restrict measurement. `alignSelf` overrides parent alignment. Test tags identify the modifier boundary, while accessibility descriptions and roles apply to the native widget.

Disabled ancestors suppress native callbacks and disable descendant widgets. Individual child disabled state is preserved when a parent becomes enabled again. Component-level `ViewNode.dispatch` also rejects a locally disabled node.

## Restoration And Cancellation

`AndroidState.encode(screen.saveState())` converts explicit saveable component state into nested Bundles. Restore with `screen.restoreState(AndroidState.decode(savedBundle))` before mounting. This includes keyed child component snapshots. Codecs serialize values explicitly and do not use reflection or Java object deserialization.

`backend.saveViewState()` and `restoreViewState(bundle)` preserve native scroll positions, lazy list position, focus, and editor selection by node type and key/position. Restore after mount; view properties are applied on the next UI turn. Stable keys are recommended when sibling order can change. Saveable component values remain the source of truth for text and selections.

Register subscriptions or asynchronous cancellation with `onDispose(...)` in a component's `onMount()`. Marshal results through `runtime.dispatch(...)`; Android scheduling is thread safe. Queued and late dispatched results are ignored after runtime closure.

## Image Loading

The default loader supports `@drawable/name`, relative asset names, `android.resource://`, `content://`, `file://`, and HTTPS. The application must declare network permission if it uses HTTPS; the library does not add it. HTTP redirects are rejected. Requests have connection/read timeouts, a 10 MiB input limit, and sampled bitmap decoding.

Pass an `ImageLoader` to the backend constructor to integrate an application image cache or other URI schemes. It returns a cancellation handle and delivers completion on the UI thread. The backend cancels changed/removed images and ignores stale completions. `Handle.imageError()` exposes load errors without logging source URLs or image values. A caller-supplied loader remains owned by the caller.

Android Java API compatibility is configured using [core library desugaring](https://developer.android.com/studio/write/java8-support) and `desugar_jdk_libs` 2.1.5, which includes [Stream.toList support](https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md).

## Verification

```sh
./gradlew -Pandroid=true :vela-android:connectedDebugAndroidTest :showcase-android:connectedDebugAndroidTest --no-configuration-cache
./gradlew -Pandroid=true :vela-android:lintDebug :showcase-android:lintDebug :showcase-android:assembleDebug --no-configuration-cache
```

The instrumented backend suite checks native editing/focus/selection, scroll retention, accessibility activation, disabled ancestors, fresh callbacks, slider steps, selectors and pickers, modal dismissal, image loading/cancellation, layout constraints, lazy materialization, supported node construction, and Bundle restoration. The showcase suite checks Activity recreation. CI configures minimum-API and recent-API emulator jobs; local results are recorded in [audit remediation](audit-remediation.md).

## Related Pages

- [Runtime guide](runtime-guide.md)
- [Components](components.md)
- [Project structure](project-structure.md)
- [README](../README.md)
