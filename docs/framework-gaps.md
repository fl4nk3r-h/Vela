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
| View model | Immutable `ViewNode` with stable keys, text, string props, children, and payload-aware event handlers |
| Built-in nodes | Broad catalog across layout, content, inputs, feedback, navigation, overlays, and data display |
| UI configuration | Immutable `Modifier`, `TextStyle`, layout enums, and semantic component variants |
| Interaction | Named events with validated payload dispatch through `ViewNode` |
| Renderer | `VelaRenderer` text tree output |
| Demo | `CounterScreen` and `App` |
| Tests | Compiler validation, view-node, modifier, component-catalog, and counter integration tests |

## High-Priority Gaps

| Gap | Why it matters | Starting point |
| --- | --- | --- |
| Automatic state invalidation | Developers must remember to call `markDirty()` after every state mutation | Generate state accessors, introduce observable state wrappers, or add a state delegate API |
| Recomposition scheduler | Callers manually check and consume dirty flags | Add a runtime that owns mounted components and schedules render passes |
| Typed props | `ViewNode` only accepts `Map<String, String>` | Introduce a typed prop model that supports values, callbacks, styles, and accessibility metadata |
| Renderer backend abstraction | Text output is useful for tests but not interactive UI | Define a renderer interface and keep text rendering as one backend |
| Tree diffing | Every render produces a full new tree without reconciliation | Add node identity, keys, and a diff model before building richer backends |
| Renderer conformance | Components expose rich layout, styling, and semantic metadata without a graphical backend | Define backend conformance tests for supported nodes and properties |
| Component composition | Components cannot directly mount child components with lifecycle boundaries | Add a composition context that can render child components and preserve state |

## State And Compiler Gaps

| Gap | Current behavior | Desired direction |
| --- | --- | --- |
| No generated code | `StateProcessor` validates only | Generate helpers or diagnostics that make correct state usage easier |
| No mutation detection | Direct field writes are invisible unless followed by `markDirty()` | Prefer generated setters or observable holders |
| Source retention only | Runtime cannot inspect state annotations | Keep source retention if generation handles state, or use runtime metadata intentionally |
| Limited processor coverage | Valid owners, indirect subclasses, and invalid fields are covered | Add fixtures alongside every new generation or validation rule |

## Runtime Gaps

| Gap | Current behavior | Desired direction |
| --- | --- | --- |
| No mounted tree | Components are plain objects | Introduce a root runtime that mounts, updates, and unmounts components |
| No lifecycle hooks | Only `onStateChanged()` exists | Add explicit mount, update, and dispose hooks if needed |
| No batching | Each clean-to-dirty transition invokes `onDirty` immediately | Batch state changes before rendering |
| No parent-child invalidation | Dirty state is local to one component | Bubble invalidation to a scheduler or root owner |
| No concurrency policy | Dirty flag is atomic, but rendering model is undefined | Define single-threaded UI semantics or documented thread-safety boundaries |

## View Tree Gaps

| Gap | Current behavior | Desired direction |
| --- | --- | --- |
| No keyed reconciliation | Nodes can carry stable keys, but no runtime consumes them | Use keys in the future tree-diff algorithm |
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

1. Decide the state API: generated setters versus observable state holder.
2. Introduce a renderer abstraction and keep `VelaRenderer` as `TextRenderer` or a compatibility wrapper.
3. Add a mounted runtime that can schedule and batch renders.
4. Define a typed prop model or stable property schema for renderer compatibility.
5. Add tree reconciliation that consumes stable node keys.
6. Build the first graphical backend and its component conformance suite.

## Candidate First Milestones

| Milestone | Outcome |
| --- | --- |
| Interactive counter kernel | Implemented: button click events call `CounterScreen.increment()` through `ViewNode.dispatch` |
| Safer state | Mutating state through the recommended API always marks the component dirty |
| Renderer contract | Text renderer and any future backend share a stable interface |
| Basic layout vocabulary | Implemented and expanded into the full catalog documented in `components.md` |

## Related Pages

- [`usage-guide.md`](usage-guide.md)
- [`components.md`](components.md)
- [`project-structure.md`](project-structure.md)
- [`documentation-guide.md`](documentation-guide.md)
