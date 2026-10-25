# Marketplace listing copy

This document is ready-to-edit listing text; it does not imply a Marketplace page exists.

**Name:** Odoo Runner  
**Publisher/author:** Oewer Solutions  
**Contact:** oewersolutions@gmail.com  
**License:** MIT (use the complete LICENSE text in the license field)  
**Source code URL:** owner will supply later  
**Vendor website/support page:** owner will supply later  
**Suggested relevant tags:** Odoo, Python, development tools, PostgreSQL

## Short description

Manage local Odoo projects and independent runners, configure environments and server options, inspect logs, browse databases and monitor runner performance in one PyCharm popup.

## Description

Odoo Runner helps developers manage multiple Odoo source checkouts and independent local runner configurations.

- Detect or create Python virtual environments and install project requirements or named packages.
- Configure Odoo server, addons, ports, modules and test options through grouped forms.
- Start, stop and restart runners; review logs, warnings and errors with copy/search controls.
- Open an interactive Odoo Shell directly in the popup.
- Browse PostgreSQL tables and technical names, inspect storage and search records. Explicit record edits use direct SQL with conflict checks.
- View CPU/memory graphs for the selected runner and activity metrics for one selected database.

This release declares PyCharm2023.2/build232 support and requires Python/Terminal support. Odoo, PostgreSQL and Python dependencies are not bundled. Interactive Shell is intended for macOS/Linux; database features require psycopg2. Database edits bypass Odoo ORM validation/computed fields/access rules. Metrics are scoped to the selected process tree/database; database statistics include other clients using that database.

Independent project by Oewer Solutions, not affiliated with or endorsed by Odoo S.A. or JetBrains. MIT licensed.

## Release notes

0.24.0 prepares the package for Marketplace submission with vendor/contact details, an MIT license, plugin-manager icons, current documentation and clean official binary-verifier results on PC-232.10072.31. It also removes the embedded terminal's internal startup API usage. Public source/vendor URLs and final submission review remain pending.

## Media to capture from the actual release

Use real screenshots after smoke verification: project/runner list, server configuration, diagnostics, database technical-name list, and Performance tab. Remove personal paths, credentials and business records. Existing design-reference mockups are not release screenshots. Do not claim unverified/newer IDE compatibility or official Odoo endorsement.
