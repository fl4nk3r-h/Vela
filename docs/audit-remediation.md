# Framework Audit Remediation

This page records the implementation and regression coverage for the 2026-10-15 framework audit.

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Implemented; verification results below |
| Audience | Maintainers and reviewers |
| Owner | Vela maintainers |
| Scope | Four high-priority and ten medium-priority audit findings |

## Changes And Coverage

| Finding | Implementation | Regression coverage |
| --- | --- | --- |
| F01: Android replaces entire UI | Shared retained `VelaAndroidBackend`, runtime mounting, and frame scheduling | Native editor identity/focus/selection, scrolling, slider identity, and Daymark button retention |
| F02: Catalog exceeds native behavior | Explicit native implementations for catalog nodes; real editing/selectors/pickers/modals; image loading; unsupported-node diagnostics | Every supported node constructs; form submission, date selection, dismissal, resource loading, and stale image cancellation |
| F03: No stateful child scopes | `child(key, stableInstance)` owns state invalidation, lifecycle, and cached child descriptions | Child changes update output without rendering parent/sibling; mount/removal/disposal tests |
| F04: Removed sibling leak | Reconciliation attempts every removed sibling and aggregates failures | First removed sibling throws; all siblings and root still disposed |
| F05: Missing restoration/cancellation | Saveable codecs/snapshots, keyed child restoration, `onDispose`, runtime dispatch, Bundle/native view restoration, scheduler cancellation | Snapshot round trip, remount, cleanup failure, late-result rejection, native selection, and Activity recreation |
| F06: Weak props/modifier contract | Typed `PropKey`/`StandardProps`, shared validation, strict Android diagnostics, max bounds/aspect/alignment/spacing/border/focus/z-index support | Invalid raw props rejected; native measurement, border, distributed spacing, aspect ratio, root parameter preservation, and lazy row resize tests |
| F07: Full-root work/eager collections | Cached child descriptions, identity-based patch/key-validation skipping, native visual update skipping, lazy list/grid factories and recycling | Unchanged subtree operation counts; independent child rendering; 10,000-item native list creates a bounded visible set |
| F08: Touch-only checked events | Native checked listeners use programmatic-update guards instead of pressed state | Accessibility click and native switch activation update application callbacks |
| F09: Inconsistent disabled inputs | Central local event guard; native inherited enabled state and callback suppression | Disabled select/slider/multiline/segments; disabled native segments and ancestors |
| F10: Ignored slider steps | Shared finite `SliderRange`, snapping, endpoint rules, native discrete indices | Decimal/negative ranges, non-divisible maximum, invalid values, native keyboard increments |
| F11: Locale-dependent tokens | `Locale.ROOT` for enum wire serialization | Turkish-locale orientation, variant, and modifier tests |
| F12: Password diagnostics | Password and explicitly sensitive subtree redaction, including keys | Direct and retained diagnostic output contains no synthetic secret values |
| F13: Missing graphical tests/CI | Core regressions, native instrumented suite, showcase recreation suite, tracked CI workflow | Local tests below; CI defines JVM and API 26/35 jobs |
| F14: SDK required for default tests | Conditional Android modules, explicit `-Pandroid` override | Default JVM tests with nonexistent SDK environment; full Android build with modules enabled |

## Scope Boundaries

The fixes add supported framework facilities and correct concrete defects; the project remains experimental. Child scopes are an explicit API: direct `child.render()` does not attach lifecycle ownership. Cached scopes and lazy collections reduce work, while arbitrary cross-owner state-read tracking and phase-specific rendering are still future work. Eager collections remain available for small data sets.

Typed property access preserves the legacy string wire map. A complete generated per-node schema is not implemented. Native widgets provide basic catalog behavior; the [capability matrix](android-backend.md) describes icon, snackbar, image, and custom-node boundaries. Backend patching is still nontransactional after arbitrary platform exceptions.

The checked-in CI configuration has not been executed remotely as part of this local task. Local device results cover the available API 36 emulator; API 26/35 coverage is configured for CI and is not represented as a local pass.

## Verification

Commands used for final verification:

```sh
./gradlew test :app:run --offline --no-daemon --no-configuration-cache
./gradlew -Pandroid=true :vela-android:connectedDebugAndroidTest :showcase-android:connectedDebugAndroidTest --offline --no-daemon --no-configuration-cache
./gradlew -Pandroid=true :vela-android:lintDebug :showcase-android:lintDebug :showcase-android:assembleDebug --offline --no-daemon --no-configuration-cache
ANDROID_HOME=/tmp/vela-missing-sdk ANDROID_SDK_ROOT=/tmp/vela-missing-sdk ./gradlew test --offline --no-daemon
```

Local results on 2026-10-15:

| Check | Result |
| --- | --- |
| JVM tests | 43 passed, 0 failures/errors/skipped |
| Native backend tests on API 36 | 15 passed, 0 failures/errors/skipped |
| Daymark Activity recreation on API 36 | 1 passed, 0 failures/errors/skipped |
| Android lint | 0 errors; 1 backend warning and 2 showcase warnings about SDK versions |
| Debug APK | Built successfully |
| SDK-free default test command | Passed with normal configuration cache enabled |

Test reports are generated under each module's `build/reports`; native XML results are under `build/outputs/androidTest-results/connected`. Lint's SDK-version notices remain visible; compile/target SDK changes need their own compatibility work. Native conformance and Activity recreation tests were run on the available API 36 emulator, not a physical device.

## Related Pages

- [Android backend](android-backend.md)
- [Runtime guide](runtime-guide.md)
- [Remaining framework gaps](framework-gaps.md)
- [README](../README.md)
