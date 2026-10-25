# Odoo Runner

Manage Odoo source projects, Python environments and multiple local runners from one PyCharm popup.

**Version:** 0.24.0  
**Author:** Oewer Solutions  
**Contact:** [oewersolutions@gmail.com](mailto:oewersolutions@gmail.com)  
**License:** [MIT](LICENSE)

This is an independent tool, not an official Odoo or JetBrains product.

## Compatibility

| Component | Requirement |
| --- | --- |
| IDE | PyCharm 2023.2, builds `232.10072.31` through `232.*` |
| IDE plugin | Bundled Terminal plugin enabled; Python support required |
| Java | Java 17 bytecode; use the IDE's compatible bundled runtime |
| Odoo | Existing source checkout and a compatible Python environment |
| Database | PostgreSQL 12+ for database browser/monitoring metadata |
| Python database driver | `psycopg2` installed in the selected project's environment |
| Operating system | macOS/Linux intended for the full feature set; interactive Shell is POSIX-only; RSS unavailable on Windows |

**Newer PyCharm branches are not declared compatible.** Do not remove or widen `until-build` to force installation. Verify and adapt the implementation first, especially the embedded Terminal APIs. No universal Odoo-version certification or Windows end-to-end certification is claimed.

For check results and remaining submission requirements, see [Marketplace readiness](docs/MARKETPLACE_READINESS.md).

## Install

1. Download/build `dist/odoo-runner-0.24.0.zip`.
2. In PyCharm, open **Settings → Plugins → gear → Install Plugin from Disk**.
3. Select the ZIP and restart PyCharm.
4. Open **Tools → Odoo Runner…**, or use the Odoo action in the top-right toolbar. Toolbar visibility depends on IDE UI/customization.

Install the distribution ZIP, not the inner JAR or a source archive. The plugin ID remains `dev.odoorunner`, preserving existing definitions when updating a previous local build.

## Create a project and environment

Choose **Add project**, select your Odoo source folder, and configure the entry script (normally `odoo-bin`). Existing virtual environments are detected in common source/sibling locations. Select an interpreter or create a new environment using a chosen base Python executable.

Creation does not install Odoo requirements automatically. Use the project's dependency icon to install a requirements file or specified Python packages into the selected virtual environment. Exact-name PyPI lookup is available on request; this is not full-text package search. Installations use that environment's `python -m pip`, not system Python. Python/Odoo/PostgreSQL services are not bundled.

Projects stay on the left and runners on the right. Drag the divider to adjust the split. Editing shared interpreter/source paths requires stopping that project's runners. Removing saved definitions retains generated files.

## Configure and run

Add runners for each project/version. New runners choose currently available HTTP/event ports excluding other saved assignments; startup checks again. Availability is not reserved between creation and launch.

Click a runner to edit:

- **Launch & tests:** database override, modules to install/update, test tags/files, development and startup options. Module selectors scan multiple standard/custom addon paths.
- **Server configuration:** grouped PostgreSQL, addon paths, directories/tools, HTTP/event ports, workers, logging, limits, mail and supported advanced options. Password fields use the IDE Password Safe.
- **Performance:** selected runner/worker CPU and summed RSS graphs; selected database transactions/connections and activity. Database metrics include all clients of that one database, not only this runner.

Save changes explicitly. The Save icon appears when the runner has changes. Controls have tooltips; Start becomes Restart while a runner is alive. Stop/restart affect the managed HTTP process. A running process does not guarantee Odoo is ready or its ports are listening.

Closing the popup leaves HTTP runners running. Closing the IDE project stops managed processes.

## Logs and Odoo Shell

The Log icon opens warnings, errors and logs in the same right panel. Filter text, pause/follow live output, inspect full messages and copy selected/filtered entries. Capture is bounded to retained history; redaction is best-effort.

The Shell icon opens a real interactive Odoo shell inside that panel using the saved interpreter/configuration and a single chosen database. It is a separate process, not attachment to the HTTP worker. HTTP binding, init/update/test launch actions are excluded. **Back ends that shell session; reopening starts a fresh one. Closing the popup ends its shell sessions too.** HTTP Stop is independent. Avoid shell transactions you do not intend to commit.

## Database browser

The database icon opens ordinary/partitioned tables, views, materialized views and foreign tables across user schemas. Enable **System objects** to include PostgreSQL catalogs and TOAST; other sessions' temporary schemas are excluded. There is no fixed table-count cutoff, but requests have timeout/response limits and fail explicitly if exceeded.

The list shows readable name, authoritative SQL technical name (`schema.table`), inferred Odoo model, schema, type, estimated rows, and local data/TOAST/index/total size. Search names or technical names such as `res_partner` / `res.partner`; combine schema/type filters. Odoo model matches use conventional dots-to-underscores mapping from `public.ir_model`; custom `_table` overrides cannot be established from that metadata. Partition sizes are per relation, not aggregated parent totals.

Open a table to browse 100 records per page and search within a chosen column or all columns. Double-click a record or use Edit to view its fields. Save only sends changed supported fields. NULL is separate from an empty string. An original primary-key/row-version check rejects conflicting edits.

**Record edits use direct SQL. They bypass Odoo ORM validation, computed fields, access rules and cache invalidation.** Database permissions still apply. Views/foreign/system objects, no-primary-key tables, protected columns and truncated rows are read-only. No insert/delete/schema-change interface is provided. Values over 100,000 characters are truncated and the record becomes read-only. Response limit: 16 MiB; pagination offset limit: 10,000,000.

## Performance details

Runner samples use the managed process and live descendants only. CPU 100% represents one core and can exceed 100% across workers. RSS sums resident pages and may count shared pages repeatedly. Short-lived workers between samples can be missed. CPU/RSS sample every 2 seconds; database activity samples every 6 seconds. Graphs cover four minutes with bounded memory. Sampling pauses and outstanding requests are cancelled while hidden; reopening starts fresh history.

Database size, client states, lock waits, cumulative deadlocks and interval cache-hit ratio belong to the selected database. Monitoring connections are excluded from connection counts, but PostgreSQL transaction counters may include observer overhead. Metrics can lag or be permission-limited. Unavailable metrics show gaps instead of fabricated zeroes.

## Data, credentials and privacy

There is no vendor telemetry, advertising or analytics endpoint. Project settings are stored by the IDE; password fields use Password Safe. Generated Odoo and shell configuration files under the opened IDE project's `.odoo-runner/` directory can contain plaintext secrets for Odoo to read. POSIX permissions are restricted where supported. These files remain after removing saved definitions.

Keep `.odoo-runner/` out of source control and do not share generated configs, full logs, personal paths or database records in support reports. Redacted previews/log filtering are not guarantees that every secret is removed. Read [Privacy](PRIVACY.md) and [Security](SECURITY.md) for actual storage, network and cleanup behavior.

## Build from source

Python 3 and a Java 17+ JDK with `javac` are required. Obtain a local PyCharm 2023.2 / build 232 SDK. The builder does not download SDKs or run package installs.

```sh
python3 build.py --ide /path/to/pycharm-2023.2 --jdk /path/to/jdk
```

The build reads its version from `src/main/resources/META-INF/plugin.xml`, compiles Java 17 bytecode and creates the installable ZIP with documentation/license. Do not bundle the IDE SDK or its libraries into the release. See [Contributing](CONTRIBUTING.md) for verification.

## Documentation and support

- [Changelog](CHANGELOG.md)
- [Privacy](PRIVACY.md)
- [Security reporting](SECURITY.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
- [Marketplace preparation and verification](docs/MARKETPLACE_READINESS.md)
- [Historical development notes](docs/DEVELOPMENT_HISTORY.md) — historical behavior may have been superseded.

Send questions and security reports to **oewersolutions@gmail.com**. Include plugin version, PyCharm build, OS, relevant sanitized error and reproduction steps. Do not include passwords, production records or unredacted configuration files.
