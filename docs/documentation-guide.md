# Documentation Guide

## Page Metadata

| Field | Value |
| --- | --- |
| Status | Active |
| Audience | Contributors and coding agents |
| Owner | Vela maintainers |
| Scope | Markdown documentation in `README.md` and `docs/` |

## Purpose

This page defines how Vela documentation is organized and maintained. It keeps the documentation useful as a small internal knowledge base: easy to scan, explicit about ownership, and traceable to the code that implements each behavior.

## Documentation Map

| Page | Use it for | Update it when |
| --- | --- | --- |
| [`README.md`](../README.md) | First visit, setup, commands, and project summary | A feature, module, command, or durable page is added |
| [`usage-guide.md`](usage-guide.md) | Public runtime API, examples, current lifecycle, and testing style | Component APIs, state behavior, renderer behavior, or examples change |
| [`project-structure.md`](project-structure.md) | Architecture, module boundaries, and runtime flow | Packages, dependencies, or core behavior change |
| [`framework-gaps.md`](framework-gaps.md) | Missing capabilities, implementation backlog, and milestone candidates | A gap is closed, reprioritized, or newly discovered |
| [`documentation-guide.md`](documentation-guide.md) | Documentation rules and maintenance workflow | The documentation contract changes |
| [`agents.md`](../agents.md) | Agent operating rules | Repository-wide contribution rules change |

## Confluence-Style Page Format

Durable reference pages should use this order:

1. Page title.
2. Page metadata table.
3. Purpose or overview.
4. Scope and ownership.
5. Main reference content, using tables where comparison helps.
6. Workflow or verification steps.
7. Related pages.

Use descriptive headings that remain stable when the page is linked from another page. Keep examples executable and use repository-relative paths.

## Maintenance Workflow

When changing the project:

1. Identify the module and behavior affected.
2. Find the page that owns that explanation using the documentation map.
3. Update the page in the same change as the code or build change.
4. Update `README.md` when the change affects onboarding, commands, modules, or the documentation map.
5. Check links, class names, paths, and commands against the repository.
6. Run the relevant verification command and record only facts that the command or source supports.

## Writing Rules

- Prefer concrete statements over marketing language.
- Explain why a module or workflow exists before listing implementation details.
- Mark future work as `Planned` or `Experimental`; do not present it as shipped behavior.
- Use one canonical explanation and link to it from other pages.
- Avoid documenting generated build output under `build/`.
- Keep headings and tables readable in both GitHub and a Confluence markdown importer.

## Verification

For source or build changes, run:

```bash
./gradlew test
```

For changes to the executable demo or rendered output, also run:

```bash
./gradlew :app:run
```

Documentation-only changes should still receive a link and command review before they are merged.

## Related Pages

- [`agents.md`](../agents.md)
- [`framework-gaps.md`](framework-gaps.md)
- [`project-structure.md`](project-structure.md)
- [`usage-guide.md`](usage-guide.md)
- [`README.md`](../README.md)
