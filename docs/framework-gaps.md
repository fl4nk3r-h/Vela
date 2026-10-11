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
| View model | Immutable `ViewNode` with type, text, string props, and children |
| Built-in nodes | `VelaText`, `VelaButton`, `VelaColumn` |
| Renderer | `VelaRenderer` text tree output |
| Demo | `CounterScreen` and `App` |
| Tests | One counter behavior test |

## High-Priority Gaps

| Gap | Why it matters | Starting point |
| --- | --- | --- |
| Automatic state invalidation | Developers must remember to call `markDirty()` after every state mutation | Generate state accessors, introduce observable state wrappers, or add a state delegate API |
| Recomposition scheduler | Callers manually check and consume dirty flags | Add a runtime that owns mounted components and schedules render passes |
| Event handling | `VelaButton` has a label but no click callback or action model | Add typed event props or an event registry on `ViewNode` |
| Typed props | `ViewNode` only accepts `Map<String, String>` | Introduce a typed prop model that supports values, callbacks, styles, and accessibility metadata |
| Renderer backend abstraction | Text output is useful for tests but not interactive UI | Define a renderer interface and keep text rendering as one backend |
| Tree diffing | Every render produces a full new tree without reconciliation | Add node identity, keys, and a diff model before building richer backends |
| Layout and styling | Column has no spacing, alignment, size, color, or typography controls | Add layout/style primitives and document their stable semantics |
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
| No stable identity | Nodes have type and props only | Add optional keys or generated identity |
| No semantic roles | Button is just a node type and label | Add semantics for accessibility and interaction |
| No custom text escaping rules | Renderer prints text directly | Define renderer-safe escaping or structured output |
| No children validation | Any node type can have any children through `ViewNode.node()` | Add component-level or renderer-level validation |
| No slots | Components cannot expose reusable content slots | Add child lambdas or component factories once Java API shape is chosen |

## Component Library Gaps

| Missing primitive | Notes |
| --- | --- |
| `Row` | Needed for horizontal layout |
| `Box` or container | Needed for stacking and decoration |
| `TextField` | Needed for input-driven samples |
| `Image` | Needed for media |
| `Spacer` | Needed for layout rhythm |
| `LazyColumn` or list | Needed for scalable collections |
| Modifiers | Needed for size, padding, click behavior, test tags, and semantics |

## Documentation And Developer Experience Gaps

| Gap | Why it matters |
| --- | --- |
| No API reference | Contributors must read source for every type |
| No architecture decision records | Major API choices will be hard to trace |
| No roadmap page | Implementation order is implicit |
| No examples beyond counter | Hard to validate API ergonomics |
| No release or compatibility policy | Public API stability is undefined |
| No contribution guide beyond agent notes | Human contributors need testing, style, and review expectations |

## Suggested Implementation Order

1. Decide the state API: generated setters versus observable state holder.
2. Add an event model for buttons because it unlocks interactive examples.
3. Introduce a renderer abstraction and keep `VelaRenderer` as `TextRenderer` or a compatibility wrapper.
4. Add a mounted runtime that can schedule and batch renders.
5. Add typed props, modifiers, and basic layout primitives.
6. Add tree identity and diffing before building a real interactive backend.

## Candidate First Milestones

| Milestone | Outcome |
| --- | --- |
| Interactive counter kernel | Button click events can call `CounterScreen.increment()` through a runtime-owned event path |
| Safer state | Mutating state through the recommended API always marks the component dirty |
| Renderer contract | Text renderer and any future backend share a stable interface |
| Basic layout vocabulary | `Column`, `Row`, `Text`, `Button`, and modifiers are documented and tested |

## Related Pages

- [`usage-guide.md`](usage-guide.md)
- [`project-structure.md`](project-structure.md)
- [`documentation-guide.md`](documentation-guide.md)
