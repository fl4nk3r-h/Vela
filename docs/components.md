# Component Reference

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Application developers, renderer authors, and framework contributors |
| Owner | Vela maintainers |
| Scope | Components, modifiers, events, semantics, and view-node construction in `vela-core` |

## Overview

Vela provides a declarative component catalog that produces immutable `ViewNode` trees. Components describe intent: layout, content, interaction, state, styling, and accessibility semantics. A renderer backend is responsible for turning that description into platform UI and dispatching platform events back to the node.

The built-in `VelaRenderer` remains a diagnostic text renderer. It exposes the complete tree, props, stable keys, and registered event names, but it does not draw graphical controls.

## Component Catalog

### Layout And Containers

| Component | Purpose | Notable configuration |
| --- | --- | --- |
| `VelaColumn` | Vertical layout | arrangement, alignment, gap, modifier |
| `VelaRow` | Horizontal layout | arrangement, alignment, gap, modifier |
| `VelaBox` | Stacked or overlaid content | content alignment |
| `VelaGrid` | Fixed-column grid | column count and gap |
| `VelaSpacer` | Explicit layout space | horizontal, vertical, or two-dimensional size |
| `VelaScroll` | Scrollable content wrapper | horizontal or vertical orientation |
| `VelaSurface` | Styled visual surface | modifier and arbitrary children |
| `VelaCard` | Grouped content surface | component variant and arbitrary children |
| `VelaScaffold` | Screen-level structure | top bar, content, and bottom bar slots |

### Content And Display

| Component | Purpose | Notable configuration |
| --- | --- | --- |
| `VelaText` | Styled text | `TextStyle` and modifier |
| `VelaImage` | Image content | source, description, and fit/crop/fill scale |
| `VelaIcon` | Named icon | icon name and content description |
| `VelaAvatar` | Profile image or initials | image/initials variants and size |
| `VelaDivider` | Visual separator | orientation and thickness |
| `VelaBadge` | Compact status label | semantic variant |
| `VelaChip` | Compact action or removable value | click and remove events |
| `VelaLink` | Navigable text | destination and open event |
| `VelaListItem` | Structured row | headline, support text, leading/trailing slots, click |
| `VelaCodeBlock` | Code or preformatted content | language and copy event |

### Inputs And Actions

| Component | Event | Payload |
| --- | --- | --- |
| `VelaButton` | `click` | none |
| `VelaIconButton` | `click` | none |
| `VelaFloatingActionButton` | `click` | none |
| `VelaTextField` | `change` | `String` |
| `VelaCheckbox` | `change` | `Boolean` |
| `VelaRadioButton` | `select` | none |
| `VelaSwitch` | `change` | `Boolean` |
| `VelaSlider` | `change` | numeric value |
| `VelaSelect` | `change` | selected `String` |
| `VelaSegmentedControl` | `change` | selected `String` |
| `VelaDatePicker` | `change` | `LocalDate` or an ISO-8601 date string |
| `VelaForm` | `submit` | none |

### Feedback And Loading

| Component | Purpose |
| --- | --- |
| `VelaProgressIndicator` | Determinate or indeterminate linear/circular progress |
| `VelaAlert` | Persistent status message with optional dismissal |
| `VelaSnackbar` | Temporary message with an optional action |
| `VelaTooltip` | Context attached to an anchor node |
| `VelaSkeleton` | Loading placeholder in rectangle, rounded, circle, or text form |
| `VelaEmptyState` | Empty-content message with illustration and action slots |

### Navigation And Overlays

| Component | Purpose |
| --- | --- |
| `VelaTopAppBar` | Screen title, navigation, and action slots |
| `VelaNavigationBar` | Navigation container and selectable navigation items |
| `VelaTabs` | Tab list and selectable tabs |
| `VelaBreadcrumbs` | Hierarchical navigation path |
| `VelaPagination` | Validated page selection |
| `VelaMenu` | Menu and selectable menu items |
| `VelaAccordion` | Expandable item group |
| `VelaDialog` | Modal content and action slots with dismissal |
| `VelaDrawer` | Start- or end-edge overlay with dismissal |

### Data Display

| Component | Purpose |
| --- | --- |
| `VelaList` | Semantic list with optional dividers |
| `VelaTable` | Validated string table with headers and rows |
| `VelaKeyValue` | Label/value metadata display |

## Layout Example

```java
ViewNode dashboard = VelaColumn.of(
    Arrangement.SPACE_BETWEEN,
    Alignment.STRETCH,
    16,
    Modifier.none().fillMaxWidth().padding(24),
    VelaText.of(
        "Dashboard",
        TextStyle.defaultStyle().fontSize(28).fontWeight(700),
        Modifier.none()
    ),
    VelaGrid.columns(
        2,
        12,
        Modifier.none(),
        VelaCard.of(VelaText.of("Revenue")),
        VelaCard.of(VelaText.of("Activity"))
    )
);
```

## Interaction Example

Components register callbacks as node events. Renderer backends dispatch the event name and payload:

```java
ViewNode field = VelaTextField.of(
    "Email",
    email,
    "name@example.com",
    VelaTextField.InputType.EMAIL,
    true,
    newValue -> updateEmail(newValue),
    Modifier.none().fillMaxWidth().testTag("email")
);

field.dispatch("change", "vela@example.com");
```

Use `dispatch("click")` for actions without a payload. `dispatch` returns `false` when the node does not register that event. Input components validate payload type and range before calling application code.

Disabled buttons do not register click handlers. Other disabled inputs retain their event description but ignore dispatched changes, allowing a renderer to expose their full semantic contract while respecting disabled state.

## Modifiers

`Modifier` is immutable. Every method returns a new value, and `then(...)` combines modifier chains with later properties taking precedence.

| Category | APIs |
| --- | --- |
| Size | `width`, `height`, `size`, min/max dimensions, `fillMaxWidth`, `fillMaxHeight`, `aspectRatio` |
| Spacing | `padding`, `margin` |
| Appearance | `background`, `foreground`, `border`, `cornerRadius`, `shadow`, `opacity` |
| Layout | `weight`, `alignSelf`, `zIndex` |
| State | `visible`, `enabled` |
| Interaction | `focusable`, `scrollable` |
| Semantics | `contentDescription`, `role`, `testTag` |
| Extensibility | `property` for renderer-specific metadata |

```java
Modifier cardModifier = Modifier.none()
    .fillMaxWidth()
    .padding(16)
    .background("#ffffff")
    .border(1, "#d0d7de")
    .cornerRadius(8)
    .shadow(2)
    .testTag("profile-card");
```

Dimensions use density-independent `dp` metadata. Font sizes use `sp` metadata. A renderer maps those units to its platform.

## Typography

`TextStyle` supports font size, weight, color, family, alignment, line height, italic, underline, and maximum lines. It is immutable and can be reused safely.

```java
TextStyle heading = TextStyle.defaultStyle()
    .fontSize(24)
    .fontWeight(700)
    .color("#111827")
    .maxLines(2);

ViewNode title = VelaText.of("Account overview", heading, Modifier.none());
```

## Variants

`ComponentVariant` provides shared semantic variants: `PRIMARY`, `SECONDARY`, `TERTIARY`, `OUTLINED`, `GHOST`, `DANGER`, `SUCCESS`, `WARNING`, and `INFO`. Renderers own their visual theme; components store only the selected semantic variant.

## Custom Components

Use the `ViewNode` builder when the catalog does not cover a domain-specific component:

```java
ViewNode chart = ViewNode.builder("chart")
    .key("monthly-revenue")
    .prop("kind", "bar")
    .modifier(Modifier.none().fillMaxWidth().height(280))
    .on("select", event -> openDataPoint(event.value()))
    .build();
```

Stable keys are optional but recommended for collection items and nodes whose identity must survive a future reconciliation pass.
Factory-built nodes can be keyed without rebuilding them:

```java
ViewNode item = VelaListItem.of(
    "Build 42", "Passing", null, null, this::openBuild, Modifier.none()
).withKey("build-42");
```

## Renderer Contract

A retained backend can implement `RendererBackend<H>` and use `MountedTree` or `VelaRuntime` to preserve host handles. See the [runtime guide](runtime-guide.md) for mounting, property/event updates, child ordering, and disposal. The Android showcase has not yet migrated to this contract.

A graphical renderer should:

1. Map each supported node type to a platform widget or drawing operation.
2. Apply `style.*`, `layout.*`, `state.*`, `interaction.*`, `text.*`, and `semantics.*` properties.
3. Preserve child order and named `slot` nodes.
4. Bind platform interactions to the names exposed by `eventNames()`.
5. Dispatch values through `dispatch(eventName, value)`.
6. Respect stable `key()` values when reconciling repeated nodes.

Unknown node types or properties should degrade predictably rather than corrupting the rest of the tree.

## Related Pages

- [`README.md`](../README.md)
- [`usage-guide.md`](usage-guide.md)
- [`runtime-guide.md`](runtime-guide.md)
- [`framework-gaps.md`](framework-gaps.md)
- [`project-structure.md`](project-structure.md)
