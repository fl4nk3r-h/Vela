# Framework Gaps

This page tracks remaining capabilities after the framework audit fixes. Implemented behavior and remaining limitations are separated explicitly.

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Maintainers and contributors |
| Owner | Vela maintainers |
| Scope | Runtime, backend, developer experience, and remaining roadmap |

## Current Baseline

Vela now provides observable state, mounted root and keyed child scopes, deferred scheduling, owner-thread checks, retained type/key reconciliation, immutable node descriptions, typed property keys with a compatible string wire map, validated shared properties, sensitive diagnostic redaction, explicit saveable state codecs, mount-owned disposal, and background-result dispatch.

The reusable `vela-android` module supplies native widgets, frame scheduling and cancellation, image loading, modals and pickers, native view restoration, and virtualized list/grid adapters. Daymark consumes this module and restores saveable screen state on Activity recreation. Unknown Android node types and unknown namespaced properties produce explicit diagnostics.

JVM regression tests and Android instrumented conformance tests are checked in. CI defines JVM verification and Android API 26/35 emulator jobs. See [audit remediation](audit-remediation.md) for local execution results and the [Android capability matrix](android-backend.md) for supported behavior.

## Remaining Runtime Work

| Capability | Current behavior | Further work |
| --- | --- | --- |
| Dependency tracking | Child scopes invalidate independently; a parent render refreshes its children | Track arbitrary cross-owner state reads and derived state subscriptions |
| Phase-specific invalidation | Cached descriptions skip reconciliation; changed descriptions patch native properties | Separate structural, layout, and paint invalidation if profiling justifies it |
| Error recovery | Failures surface; removal and scope cleanup continue across failures | Define error boundaries and recovery after partially applied backend mutations |
| Effects | `onDispose` owns cancellation; `dispatch` marshals results | Higher-level task/effect APIs and pause/resume semantics |
| State migration | Explicit codecs and keyed snapshots | Versioned application migrations and schema evolution |
| Scheduler portability | Generic deferred contract; thread-safe Android adapter | Other platform adapters and standard cancellation tokens |

## Remaining View And Backend Work

| Capability | Current behavior | Further work |
| --- | --- | --- |
| Typed schema | Typed property keys and shared validation; legacy `props()` uses string wire values | A complete per-node schema and generated backend contracts |
| Collection identity | Lazy adapters use stable keys; eager nodes use keyed reconciliation | More collection-level diagnostics and stable data-diff updates |
| Styling | Native basic styles and public modifier support | Rich themes, animation, spans, advanced typography, and visual regression coverage |
| Accessibility | Native controls, descriptions, roles, keyboard/accessibility activation tests | Broader assistive technology/device coverage and richer collection announcements |
| Icons | Small native glyph mapping | Pluggable icon packs and vector resources |
| Snackbar | Application-controlled inline status/action | Optional timed overlay behavior |
| Custom nodes | Explicit unsupported-node diagnostics on Android | Extension registration and conformance contracts for custom widgets |
| Other platforms | Text and Android backends | Desktop, browser, or terminal adapters |
| Media and drawing | Images and custom node descriptions | Standard audio/video, canvas/charts, drag-and-drop, and file picker contracts |

## State Annotation

`@State` remains validation-only and source-retained. It does not observe raw field mutation or generate setters. New code should use observable holders. In-place mutation of objects already held in state is not observed; replace immutable values through `set` or `update`.

## Release And Performance Work

- The project is experimental and has no stable API guarantee yet. Public API changes must be documented and covered by regression tests.
- Native adapters and new composition APIs need broader application examples and device coverage.
- Operation-count tests and lazy-materialization tests provide regression bounds; frame-time/allocation benchmarks and performance tracing remain future work.
- Maven publication, signed releases, and an external compatibility policy are not configured.
- CI workflow execution on GitHub follows normal repository push/PR operations; adding the workflow locally does not establish a remote green run.

## Suggested Next Milestones

1. Expand minimum-API and accessibility coverage, plus visual regression fixtures.
2. Add real application benchmarks for editing, scrolling, large lists, and child-scope updates.
3. Design typed node schemas and custom-widget extension contracts while retaining wire compatibility.
4. Add error boundaries and higher-level lifecycle-aware tasks based on application needs.
5. Establish release artifacts, compatibility expectations, and contribution guidance before external adoption.

## Related Pages

- [Audit remediation](audit-remediation.md)
- [Android backend](android-backend.md)
- [Runtime guide](runtime-guide.md)
- [Component reference](components.md)
- [README](../README.md)
