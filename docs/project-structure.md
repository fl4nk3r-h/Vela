# Project Structure

This repository is organized around a simple three-module split so the framework code stays separate from the demo app.

## `vela-core`

The core module is the shared runtime surface.

### Responsibilities

- Define the component base class.
- Model the virtual UI tree.
- Track dirty state and re-render requests.
- Provide small component factories for text, button, and column nodes.

### Key Types

- `dev.vela.core.VelaComponent`
- `dev.vela.core.ViewNode`
- `dev.vela.core.VelaRenderer`
- `dev.vela.core.StateManager`
- `dev.vela.annotation.State`
- `dev.vela.components.VelaText`
- `dev.vela.components.VelaButton`
- `dev.vela.components.VelaColumn`

## `vela-compiler`

The compiler module is reserved for annotation processing.

### Responsibilities

- Validate `@State` usage at compile time.
- Keep generation logic isolated from runtime classes.
- Provide the home for future code generation and metadata extraction.

### Key Type

- `dev.vela.processor.StateProcessor`

## `app`

The app module is the demo and integration surface.

### Responsibilities

- Host the executable entry point.
- Demonstrate a real component screen.
- Exercise the rendering and dirty-state flow.

### Key Types

- `dev.vela.app.App`
- `dev.vela.app.CounterScreen`

## Working Rules

- Put shared framework code in `vela-core`.
- Put compile-time checks and generators in `vela-compiler`.
- Put demo or product-specific screens in `app`.
- Keep names domain-specific. Avoid dumping unrelated helpers into broad catch-all packages.
