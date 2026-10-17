# Agent Instructions

## Purpose

This file is the working agreement for agents contributing to Vela. Read it before changing source code, build configuration, tests, or documentation.

## Repository Context

Vela is a Java 25 Gradle workspace with Java 17 shared runtime bytecode and Java 21/25 JVM compatibility checks.

| Module | Owns | Does not own |
| --- | --- | --- |
| `vela-core` | Runtime components, state tracking, view nodes, and rendering | Annotation-processing rules or demo-only behavior |
| `vela-compiler` | Compile-time validation for `@State` | Runtime state management or rendering |
| `app` | Executable demo and integration examples | Shared framework APIs |
| `vela-android` | Native rendering and Activity lifecycle host | Build-time YAML parsing or debug code loading |
| `vela-tooling` | YAML validation, resource generation, and application configuration | Android runtime dependencies |
| `vela-gradle-plugin` | Cacheable generation, application bootstrap, and reload compilation | Application screen logic |
| `vela-cli` | Project creation, managed setup, build commands, and development watcher | Renderer implementation |
| `vela-devtools-android`, `vela-dev-protocol` | Debug reload transport and code loading | Release application dependencies |

Keep changes in the narrowest module that owns the behavior. Update tests with behavior changes and keep public API changes explicit in the documentation.

## Documentation Contract

Treat `docs/` as a set of Confluence-style project pages, not as a scratchpad.

- Start each page with a clear title and a short page-purpose statement.
- Add a `Page metadata` table with status, audience, owner, and scope when the page is a durable reference.
- Use stable headings, short paragraphs, tables for structured comparisons, and fenced code blocks for commands or trees.
- Link related pages and the root `README.md`; do not duplicate a complete explanation in multiple places.
- Describe behavior that exists in the repository. Label planned or experimental behavior explicitly.
- When a change affects module boundaries, public APIs, commands, tests, or user-visible behavior, update the relevant docs in the same change.
- Update the root README when adding a feature, module, command, or durable documentation page.
- Preserve ASCII unless the existing document already requires another character set.

## Documentation Locations

- `README.md`: entry page, requirements, quick start, and high-level project map.
- `docs/project-structure.md`: module ownership, package map, dependency flow, and runtime flow.
- `docs/documentation-guide.md`: documentation conventions and page maintenance workflow.

## Engineering Workflow

1. Inspect the owning module, nearby tests, and the relevant documentation before editing.
2. Make the smallest focused change that preserves existing public APIs unless the request requires otherwise.
3. Add or update tests for changed behavior.
4. Run `./gradlew test` for Java or Gradle changes.
5. Run `./gradlew :app:run` when the demo flow or rendered output changes.
6. Update documentation and verify every command, path, class name, and module claim against the repository.

## Review Checklist

- Does the change live in the correct module?
- Are mutable state changes marked dirty and covered by a test?
- Are `@State` compiler rules still accurate?
- Are docs linked, current, and explicit about implemented versus planned behavior?
- Are tests and the relevant Gradle commands passing?
