# Changelog

## 0.24.0 — Marketplace preparation

- Oewer Solutions vendor metadata and support email.
- MIT license and original light/dark plugin-manager icons.
- Current README, privacy/security documentation, third-party notices and publishing notes.
- Explicit Python/Terminal dependencies and restart requirement.
- Replaced the internal/deprecated terminal startup API, deprecated run-content lookup and deprecated URL construction; official verifier is clean on PC-232.10072.31.
- Release version read from plugin.xml; license and user docs included in the ZIP.

## 0.23.0 — Runner performance

- Runner/worker CPU and RSS graphs and selected-database activity graphs.
- Database session/size/cache/lock/deadlock status, scoped sampling and cleanup when hidden.

## 0.22.0 — Database table coverage and technical names

- Views/materialized/foreign tables, system-object toggle and no fixed table-count cutoff.
- Qualified SQL names, inferred Odoo model names, search/filtering and storage breakdown.

## 0.21.0 — Database browser

- Table/record browsing, column filtering and explicit single-record SQL editing with conflict checks.

## 0.20.1 — Shell lifetime

- Back from Odoo Shell ends its session.

## 0.20.0 — Embedded Shell

- Interactive Odoo Shell in the popup's right panel.

## Earlier local development releases

Project/venv setup, independent runner ports, UI-managed configuration, addon/module selection, dependency installation, logs/diagnostics and native-theme UI refinements. See docs/DEVELOPMENT_HISTORY.md for historical details; PROJECT_MEMORY.md records implementation/verification limits.
