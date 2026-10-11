# Framework Gaps

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Maintainers and implementation agents |
| Owner | Vela maintainers |
| Scope | Missing framework capabilities observed in the current repository |

## Overview

This page lists the practical gaps between Vela's current kernel and a more complete Compose-like framework. It is intentionally implementation-facing: each gap describes what is missing, why it matters, and a likely starting point.

## Current Baseline

Vela currently has:

| Capability | Current implementation |
| --- | --- |
| Component base class | `VelaComponent` with `render()`, `markDirty()`, `isDirty()`, and `consumeDirty()` |
| State annotation | `@State` with source retention |
| State validation | `StateProcessor` rejects non-field usage, final fields, and fields outside a `VelaComponent` |
| Dirty tracking | `StateManager` stores a single atomic dirty flag per component |
| Observable state | `MutableState<T>` automatically invalidates its owner and skips equal writes |
| Mounted runtime | `VelaRuntime` batches root updates, enforces its thread, and runs root lifecycle hooks |
| Reconciliation | `MountedTree` retains handles by type/key and validates duplicate sibling keys |
| View model | Immutable `ViewNode` with stable keys, text, string props, children, and payload-aware event handlers |
| Built-in nodes | Broad catalog across layout, content, inputs, feedback, navigation, overlays, and data display |
| UI configuration | Immutable `Modifier`, `TextStyle`, layout enums, and semantic component variants |
| Interaction | Named events with validated payload dispatch through `ViewNode` |
| Renderer | `RendererBackend` contract, retained `TextRendererBackend`, and direct `VelaRenderer` output |
| Demo | `CounterScreen` and `App` |
| Tests | Compiler, view-node, modifier, catalog, state, scheduling, reconciliation, cleanup, and counter integration tests |

## High-Priority Gaps

| Gap | Why it matters | Starting point |
| --- | --- | --- |
| Fine-grained recomposition | Observable state invalidates the whole root component | Track reads in child composition scopes and skip unchanged subtrees |
| Platform frame integration | The runtime has a deferred scheduler contract, but no platform adapter | Bind Android scheduling to its UI frame loop and lifecycle |
| Typed props | `ViewNode` only accepts `Map<String, String>` | Introduce a typed prop model that supports values, callbacks, styles, and accessibility metadata |
| Android backend migration | Core backend contracts exist; the Android showcase still recreates its hierarchy | Adapt native view creation, property updates, child ordering, and disposal to `RendererBackend` |
| Phase-specific invalidation | The core retains host handles but traverses the whole root tree | Separate structural, layout, and paint updates after profiling |
| Renderer conformance | Components expose rich layout, styling, and semantic metadata without a graphical backend | Define backend conformance tests for supported nodes and properties |
| Component composition | Components cannot directly mount child components with lifecycle boundaries | Add a composition context that can render child components and preserve state |

## State And Compiler Gaps

| Gap | Current behavior | Desired direction |
| --- | --- | --- |
| No generated code | `StateProcessor` validates only | Generate helpers or diagnostics that make correct state usage easier |
| No raw-field mutation detection | Direct annotated field writes still require `markDirty()` | Prefer observable holders; explicitly decide whether legacy setters should be generated |
| Source retention only | Runtime cannot inspect state annotations | Keep source retention if generation handles state, or use runtime metadata intentionally |
| Limited processor coverage | Valid owners, indirect subclasses, and invalid fields are covered | Add fixtures alongside every new generation or validation rule |

## Runtime Gaps

| Gap | Current behavior | Desired direction |
| --- | --- | --- |
| No child component scopes | The runtime mounts one root component with a view tree | Preserve child component state and lifecycle by stable identity |
| No lifecycle-bound effects | Root mount/unmount hooks exist without an effect/cancellation API | Add scoped work, cancellation, and state restoration |
| No physical callback cancellation | Closing invalidates queued callbacks but cannot remove them from a platform queue | Extend platform adapters with cancellation where useful |
| No read dependency tracking | Dirty state invalidates the root owner | Track child subscriptions and derived values |
| No background-state integration | Mounted state is thread-confined; unmounted state remains caller-managed | Marshal background results through a documented UI dispatcher |

## View Tree Gaps

| Gap | Current behavior | Desired direction |
| --- | --- | --- |
| Limited backend conformance | Keyed retention is tested using text and recording backends | Verify real native focus, cursor, scrolling, and gesture retention |
| No semantic backend | Components expose roles, labels, and descriptions | Map semantics to platform accessibility APIs in each renderer |
| No custom text escaping rules | Renderer prints text directly | Define renderer-safe escaping or structured output |
| No children validation | Any node type can have any children through `ViewNode.node()` | Add component-level or renderer-level validation |
| No lazy content factories | Named slot nodes exist, but collection content is created eagerly | Add keyed item factories once the mounted runtime exists |

## Remaining Component Library Gaps

| Missing capability | Notes |
| --- | --- |
| Virtualized collections | `VelaList` and `VelaGrid` describe content but do not lazily mount large data sets |
| Rich text and editing | Styled text exists, but spans, selection, and document editing do not |
| Canvas and charts | Custom node construction is available, but no drawing API is standardized |
| Media playback | Images are described; audio and video lifecycle APIs are not defined |
| Drag and drop | The event model exists, but pointer gestures and transfer payloads are not standardized |
| Platform pickers | File, color, and time pickers require backend capability contracts |

## Documentation And Developer Experience Gaps

| Gap | Why it matters |
| --- | --- |
| No architecture decision records | Major API choices will be hard to trace |
| No roadmap page | Implementation order is implicit |
| No examples beyond counter | Hard to validate API ergonomics |
| No release or compatibility policy | Public API stability is undefined |
| No contribution guide beyond agent notes | Human contributors need testing, style, and review expectations |

## Suggested Implementation Order

1. Adapt the Android renderer to the retained backend contract and a platform frame scheduler.
2. Define typed props and stable layout/modifier semantics.
3. Complete text editing, images, semantics, and native backend conformance.
4. Add child component composition, dependency tracking, saveable state, and effects.
5. Add lazy collections and phase-specific invalidation.
6. Establish release benchmarks, tracing, and performance regression gates.

## Candidate First Milestones

| Milestone | Outcome |
| --- | --- |
| Interactive counter kernel | Implemented: button click events call `CounterScreen.increment()` through `ViewNode.dispatch` |
| Safer state | Implemented for observable holders; raw annotated fields remain manually invalidated |
| Renderer contract | Implemented in core and the retained text backend; Android migration remains planned |
| Mounted runtime | Implemented for one root with deferred scheduling, keyed retention, and teardown |
| Basic layout vocabulary | Implemented and expanded into the full catalog documented in `components.md` |

## Related Pages

- [`usage-guide.md`](usage-guide.md)
- [`components.md`](components.md)
- [`runtime-guide.md`](runtime-guide.md)
- [`project-structure.md`](project-structure.md)
- [`documentation-guide.md`](documentation-guide.md)
