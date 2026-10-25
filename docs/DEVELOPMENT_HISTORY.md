# Odoo Runner — 0.17.0

Standalone PyCharm plugin for Odoo projects/versions with multiple runners per project. Each runner has its own launch options, ports, generated configuration and data directory.

Targets **PyCharm 2023.2, builds 232.10072.31–232.***, Java 17. Other IDE branches are not yet supported.

## Install and open

In **Settings → Plugins → gear → Install Plugin from Disk**, select `dist/odoo-runner-0.17.0.zip`. Restart if requested; it updates earlier versions using the same plugin ID and saved definitions.

Click the **Odoo wordmark in the top-right main toolbar**. The action is registered in `MainToolbarRight` for the New UI and `NavBarToolBarOthers` for the classic navigation bar. Classic UI requires the navigation bar to be visible. Toolbar placement still needs runtime validation and depends on toolbar customization/available space. Fallback: **Tools → Odoo Runner…** or Find Action → Odoo Runner.

The UI opens as one resizable, modeless popup per IDE project, with a larger initial content size up to 1280 × 860, reduced to fit the usable screen area. It remembers its size under a new dimension key so older small popup dimensions do not override the new initial size. Closing it leaves runners working; reopening shows their current process state.

## Projects tab

The sidebar contains Projects/Settings navigation and project search by name/source path. The Projects view uses a persistent **left project panel (30%)** and **right runner panel (70%)**. Drag the divider to adjust their widths; the divider proportion is saved with this IDE project. Both lists scroll with the mouse wheel/trackpad while scrollbar chrome stays hidden.

The left list shows every configured Odoo project/version, runner count, running process count and running HTTP/event port pairs; hover a project to see its source directory. Selecting a project displays its runners on the right without hiding the project list. The selected project has a summary header with total/active runner statistics; runners appear in rounded cards with right-aligned icon controls. Each project row has **Edit** (pencil) and **Delete** (trash) icons at its right edge. They act on that row’s project after navigation/unsaved-change checks; Delete removes the saved definition after confirmation and retains files on disk. Icons have tooltips and accessible names. It refreshes once per second while the popup is open. Only processes started by this plugin in the current IDE session are counted. Running means the process is alive, not that Odoo has completed startup or is serving requests. Event ports are configured values and may not actually be listening, particularly with workers=0.

Select a project in the left panel to view its runners on the right. **Add project** and the project-row **Edit** icon open a project setup page inside this same popup. Configure its name, existing source directory and Odoo entry script (usually `odoo-bin`). Path fields include Browse buttons. Editing shared source/interpreter paths requires stopping that project's runners.

Choosing or typing the source directory automatically detects virtual environments after a short delay. Discovery checks the source directory itself, `.venv`, `venv`, `env`, `.env`, up to 200 immediate child directories for custom environment names, and the four common environment names beside the source directory. A recognized environment has `pyvenv.cfg` and an executable `bin/python`, `bin/python3` or `Scripts/python.exe`. It selects the first detected environment for a new source; editing an existing project retains its saved interpreter. Use the interpreter dropdown to choose another detected environment or Browse to select any existing interpreter. Detect environments refreshes the results manually. Discovery is a bounded filesystem check, not a runtime Python/requirements verification; it does not search arbitrary external environment stores.

To create an environment, enable **Create a new virtual environment**. Select the desired base Python executable from the dropdown, browse to it, or type an absolute path. Candidates come from configured defaults, PATH, and common system directories; labels show executable paths, not runtime-verified versions. Choose the version compatible with your Odoo source.

Enter a new environment directory (default `<source>/.venv`) and press **Create virtualenv**. Its parent must exist, and the target itself must not exist. Creation runs the selected interpreter as `python -m venv <target>` in the background, without a shell. It refuses existing targets atomically, selects the created environment on success, and displays output/errors inline when finished. The base Python must provide `venv`/`ensurepip`; missing support is reported in the output. Odoo requirements are **not installed automatically**.

Creation has a three-minute timeout. Cancel creation requests termination of the process tree; partial files remain on failure, timeout or cancellation. Wait for creation/cancellation to finish before leaving setup or closing the popup. Save project commits the definition; Cancel project setup discards unsaved form changes but retains any created environment. Project setup changes also prompt before closing. File/directory pickers use the plugin path browser described below; project setup stays in the original popup.

Selecting a project shows its **runner list in the right panel**. Each runner row displays its name, configured HTTP/event ports, effective database selection and process status, including the exit code after termination. The list refreshes once per second without rebuilding unchanged rows or replacing focused buttons. Click a runner row to open its configuration. Rows are keyboard-focusable; Enter/Space opens the form. There is no separate Settings button. At the right end of each row there are four icon buttons:

- **Stop** (square): requests process termination and cancels a queued restart.
- **Restart** (circular arrow): starts an inactive runner or stops/waits/relaunches an active one using saved configuration.
- **Warnings/errors** (triangle): opens diagnostic output on the right. Its tooltip shows the number of captured warning/error lines.
- **Log** (document): opens the full captured output on the right.

The status text/dot is green for Active, gray for Inactive and amber for Stopping/Restarting. Active means process alive, not server readiness. Tooltips retain the detailed process status, exit code or failure message.

Restart uses the saved odoo-bin arguments and odoo.conf settings, including module install/update/test options. It waits in the background for up to15 seconds before replacement; if the old process has not exited, replacement is cancelled and the status tooltip explains why. Stop cancels pending replacement. Repeated restarts are disabled while pending. Pending work belongs to the IDE project and continues if the popup closes; project closure cancels it. Port conflicts are rechecked on launch.

Add runner creates a runner and opens its configuration. Stop an active runner before saving changes. Use ← Runners to return to the list; Remove runner is in the configuration form. Each runner retains these two configuration sections:

### odoo-bin · Launch & tests

Shows Python, source entry script and working directory. Configure:

- Database override (`--database`), taking precedence over the config's database names.
- Install modules (`--init`) and update modules (`--update`) using **Select modules…**. The searchable checkbox list comes from your configured standard and custom addon folders; Apply selection fills the launch option.
- Run tests (`--test-enable`), test tags (`--test-tags`) and test file (`--test-file`).
- Stop after initialization (`--stop-after-init`).
- Development features (`--dev`) and demo modules to skip (`--without-demo`).

Use flags supported by your selected Odoo version. For example, test tags can select `/sale:TestSale.test_confirm` or `standard,-slow`. Tests normally run during module install/update. Enable Stop after initialization for a one-off update/test run. Saved install/update/test options are applied again on each Run. Demo-data flags differ between Odoo versions; there is no automatic version-specific option catalog yet.

### odoo.conf · Server configuration

Configure runner name, HTTP/event ports, event option (`gevent_port` or `longpolling_port`), database names, PostgreSQL host/port/user, workers and optional absolute data directory. Blank data directory creates isolated storage for the runner. For ordinary development, use workers=0 initially.

**Multiple addon folders:** Standard/enterprise and Custom addon sections each support multiple folder selection, removal and ordering with ↑/↓. Standard folders are searched first, followed by custom folders; both are combined into one de-duplicated `addons_path` in the generated configuration. Duplicate module names resolve to the first configured root. Commas/line breaks are unsupported in folder names because Odoo uses a comma-delimited config value. Existing addon entries migrate into the standard list; relative legacy paths resolve against the Odoo source and are normalized to absolute paths on Save. Save validates that each folder exists. New runners infer existing `<source>/odoo/addons` and `<source>/addons` folders; add your enterprise/custom folders explicitly.

**Module discovery:** Select modules in the launch form scans the current UI addon-folder choices, including unsaved path changes. It lists direct child folders containing `__manifest__.py` or legacy `__openerp__.py`, with module technical name and source path. It does not import or execute manifest code or connect to PostgreSQL, so the list describes source availability, not database installation state or installability. Search filters names/paths; Select visible/Clear visible modify checkboxes for the current search. Previously saved names that are absent from the scan remain selected and are labeled as missing so they are not silently lost. Apply selection updates the form; Save persists it. A scan failure disables Apply; fix folder permissions/paths and reopen the picker to retry.

**Available ports:** HTTP/event fields are editable dropdowns. Find available probes loopback TCP ports near the configured starting HTTP port (up to 500 candidates, returning up to 40 free ports), excluding other saved runners' assignments and the current other port. Select a result or enter a custom port. The current value is retained even if it was not found free; the scan makes no reservation. Run/Rerun/Restart recheck ports before launching. This is an advisory localhost scan, not a guarantee across interfaces or concurrent processes.

**Grouped configuration:** The odoo.conf form is organized into Runner; Database configuration; Addons and directories (including Data and PostgreSQL tools); HTTP/event ports and reverse proxy; Workers and resource limits; Logs; Outgoing mail; Master password; and Additional options. The launch form is organized into source paths, database/module operations, tests, development/startup and notes. Both use a single vertical, viewport-width-tracking form rather than horizontally oversized content. Hidden-scrollbar wheel handling uses precise wheel movement directly; inner addon lists pass scrolling to the surrounding form at their boundaries. Runtime mouse/trackpad behavior still needs user validation.

**PostgreSQL tools:** `pg_path` has a folder picker for the directory containing PostgreSQL executables (such as pg_dump/pg_restore); it is distinct from the database server host/port. A supplied path must be an existing absolute directory. `db_template`, SMTP server/port/user/SSL/from-address and database logging now have dedicated grouped fields. Existing advanced entries migrate into these controls on load.

**Deployment fields:** Database filter (`dbfilter`), HTTP interface, database connection limit, cron threads, worker memory/time/request limits, logfile/log handler, proxy mode, database listing and database SSL mode have dedicated controls. Blank deployment values omit the option and retain Odoo defaults (the plugin still defaults HTTP interface to localhost). Memory limits use bytes; time limits use seconds. Existing values for these options migrate from advanced rows into the dedicated fields. Remaining options stay in the editable key/value table. These controls follow [Odoo 20 system configuration](https://www.odoo.com/documentation/20.0/administration/on_premise/deploy.html); they generate Odoo settings, and do not provision PostgreSQL, Nginx, HTTPS or proxy routing.

Use the dedicated database/admin/SMTP password fields. Blank retains stored values; Clear saved passwords on Save deletes all three stored passwords for this runner. Stored passwords are never revealed in the UI.

The Additional odoo.conf options table provides editable key/value rows with Add option and Remove option controls for other `[options]` entries. Managed form fields and the three password fields cannot be duplicated here. Option names are checked for syntax/duplicates, not against the selected Odoo version. A deployment value cannot also be repeated in the additional-options table. Existing advanced option text from 0.1.0 loads into this table; comments and blank lines are omitted.

### Save and run

Save persists both configuration sections. Preview command and Preview odoo.conf save stopped runners and show their resulting command/config; passwords are masked. For active runners, previews show saved settings. Unsaved runner changes prompt before returning to the runner list or closing the popup.

Run saves, checks ports, generates `odoo.conf` and launches Odoo in the native IDE Run console. Stop requests termination. Open browser opens the saved HTTP port. Stop an active runner before changing its settings, and stop a project's runners before editing its source/interpreter. Each runner can run concurrently with others when their ports are different.

Example: Odoo 17 / Customer A on HTTP 8069 and event 8072; Odoo 17 / Customer B on 8079 and 8082; Odoo 18 / Development on 8089 and 8092.

## Runner diagnostics and logs

The warning/error and log icons open Diagnostics **inside the existing right panel**, replacing its current runner view. There is no extra right-hand pane or divider. Use ← Runners to return.

Diagnostics provides **Warnings**, **Errors** and **Log** tabs. Warnings/errors are displayed one line per table row with line number, severity, source and message. Filter lines matches literal text in the message/source separately within each severity tab; headers support sorting and rows can be selected/copied using standard Swing table keyboard actions. Hover a clipped message to read its full text. Counts show matching rows in the retained history, not unique incidents. Traceback continuation lines follow their error severity.

The Log tab appends new output incrementally instead of resetting the entire document every refresh. Diagnostic updates are coalesced on a100ms UI timer; configured-file polling also runs every100ms. This replaces the previous independent one-second delays. Actual event-to-display latency is not benchmarked and still depends on Odoo flushing complete lines, OS/file I/O and IDE load. Follow new output controls automatic scrolling. Version0.17.0 uses the supplied reference for layout, rounded outlines, spacing and hierarchy only. There are no custom background fills or color overrides for native inputs/selections. Native PyCharm theme surfaces and popup chrome are retained.

Capture begins when a runner is started by this plugin and retains output in memory while the popup is closed. It collects stdout/stderr; if a `logfile` is configured, a background reader also follows bytes appended to that file after launch, handling truncation and ordinary rotation. Relative log paths resolve against the Odoo source folder. Existing file contents are not imported. Logs from instances started before updating this plugin must be captured by stopping/restarting those runners.

Warning/error filtering recognizes WARNING/WARN/ERROR/CRITICAL/FATAL and traceback continuations. It is a text heuristic, not a Python exception parser. The tooltip counts retained-generation warning/error lines (including continuation lines), not unique incidents. The structured diagnostic history and full log retain up to2,500 lines; the legacy combined issue snapshot used internally retains up to1,200 lines. Individual partial lines are bounded. The logfile reader limits catch-up per poll and marks skipped older data during large bursts. Capture resets on each runner start/restart and is lost when the IDE project closes. Logs are not written to workspace settings or project memory.

Known runner passwords and basic URL username/password combinations are redacted in this capture. Other application/business output is shown as emitted; the native IDE console remains separate. An unconfigured logfile produces console capture only. This initial version does not query database-backed `ir.logging`, external log aggregators or arbitrary historic log files.

## Install project dependencies

Each project row has an **Install pip dependencies** package icon. It opens a form inside the popup showing the selected project Python executable and defaults to `<source>/requirements.txt`; choose another requirements file if needed.

Press **Install requirements** to run the selected virtualenv Python as `python -m pip --require-virtualenv install --disable-pip-version-check --no-input --requirement <file>`. The interpreter must belong to a conventional virtualenv with `pyvenv.cfg`; system Python installs are refused. Pip also checks that it is running in a virtualenv. The plugin disables user-site and environment target/prefix overrides for this command.

Stop that project's runners before installation. While pip is running, project edits/deletion and runner starts are blocked. Output/status and cancellation are available in the form. Closing/navigating away from the popup does not stop installation; reopen the project dependency form to view it. Closing the IDE project requests process termination. Cancelling or failing an installation may leave packages already changed; there is no rollback. Pip may download/build packages according to the selected requirements and pip configuration. This action does not install PostgreSQL/system libraries or automatically install dependencies on project creation.

## File and folder browser

Browse actions now use a resizable plugin chooser with Back/Forward history, Up/Home navigation, a path field and Go/Enter, and a search field filtering names/paths within the current directory. Enter absolute paths, `~/...` or paths relative to the current directory. Double-click a folder (or Enter) to navigate. Choose selects the highlighted entry or the current folder in folder mode; a different typed valid path takes precedence over an old highlighted selection.

Addon-folder browsing supports multiple selections in one directory. Source directories, data/pg paths, Python executables, entry scripts and venv-base browsing share the same browser. Search is local to the currently opened folder, not recursive across the disk. Directory reads run in the background and show up to20,000 matching-type entries; use a more specific path for larger directories. Hidden files remain visible. Cancelling the chooser leaves the original field unchanged. UI navigation/search acceptance remains pending user validation.

## Settings tab

Settings is a placeholder: “Settings will be planned later.” Configuration controls are removed from this tab. Odoo server and launch configuration stays inside each runner’s Settings form. Previously saved internal defaults are retained for compatibility; they are no longer editable through the Settings tab.

## Storage and lifecycle

Definitions/defaults are local to the open PyCharm project, stored in workspace settings. The project selector can contain external Odoo source directories; it does not switch IDE project windows or provide a global catalog.

Files are generated in `<opened-project>/.odoo-runner/<runner-uuid>/odoo.conf`; default data storage is adjacent `data/`. Add `.odoo-runner/` to the opened project's `.gitignore`. Use distinct database names for business data isolation: different ports do not isolate PostgreSQL data. Blank database selection leaves behavior to Odoo.

Passwords use PyCharm Password Safe and the IDE's storage policy. Odoo needs those passwords in its generated config, which remains on disk after stopping/removing definitions. POSIX directories receive 0700 and config files 0600. Never commit/share generated configs. Other advanced values are ordinary workspace settings; do not enter secrets there. Removing definitions retains generated files and Password Safe records.

Port checks cover active assignments in this IDE project and loopback socket availability; checks reserve nothing and can race with other processes. The event port is checked even with workers=0. Project close requests process termination. Process launch does not establish readiness or database connectivity; inspect console output.

Not implemented: native Python debugger integration, existing config import/edit, custom environment UI, Docker/SSH runners, global cross-window catalog. Requirements-based pip installation is available as the explicit project action described above. Existing Odoo installations are not modified by configuration editing; launching with module/test options performs the requested Odoo operations.

## Build and verification

```sh
python3 build.py --ide /path/to/pycharm-2023.2-sdk --jdk /path/to/jdk
```

Uses a local build-232 SDK and JDK 17+ without Gradle/SDK downloads. Output: `dist/odoo-runner-0.17.0.zip`.

2026-10-24: Java compilation and ZIP packaging succeeded against local PyCharm Community 2023.2.3 SDK targeting Java 17. Compiler notes a deprecated IntelliJ API. No automated suites, browser flows, IDE installation, Odoo launches, credential operations or runtime UI checks were performed. Toolbar placement, larger popup sizing, grouped form presentation, divider persistence, wheel/trackpad and nested-scroll behavior, environment detection/creation/cancellation, addon/module scanning, port selection and runner lifecycle behavior need user validation. No actual virtual environment creation, addon scan, port probe or Odoo launch was executed during development.

References: [JetBrains actions](https://plugins.jetbrains.com/docs/intellij/basic-action-system.html), [JetBrains dialogs](https://plugins.jetbrains.com/docs/intellij/dialog-wrapper.html), [JetBrains execution](https://plugins.jetbrains.com/docs/intellij/execution.html), [Odoo 17 CLI options](https://www.odoo.com/documentation/17.0/developer/reference/cli.html), [Odoo testing](https://www.odoo.com/documentation/19.0/developer/reference/backend/testing.html).


Deployment reference note: the exact English Odoo 20 deployment URL timed out during this change. Relevant options were read from the corresponding official [German deployment guide](https://www.odoo.com/documentation/20.0/de/administration/on_premise/deploy.html), [Spanish guide](https://www.odoo.com/documentation/20.0/es_419/administration/on_premise/deploy.html) and [Odoo 20 CLI reference](https://www.odoo.com/documentation/20.0/de/developer/reference/cli.html) search content. Runtime support still depends on the Odoo source version selected in your project.


## 0.17.0 form layout

Text buttons share a168×36 size; icon-only buttons are36×36. Equal-width action rows wrap when the available panel width is insufficient. The runner form has a breadcrumb/back control, runner summary/status, right-aligned removal action, rounded section outlines, bold headings and aligned field labels. Dialog Close/Choose actions use the same button sizing. These changes use the supplied image for layout only: no custom background fills are applied. Visual sizing remains pending validation in PyCharm.


Version0.17.0 refines the supplied reference with teal accent outlines/foregrounds, red Remove runner outline, matching action/section icon types, helper text and individually aligned monospaced source paths. Native backgrounds are retained throughout; cards and badges paint outlines only. Exact pixel/OS appearance remains unverified in the IDE.


## 0.17.0 sizing and top actions

Single-line form inputs/dropdowns are320×36, text action buttons168×36, and icon actions36×36. Browse/module/port compound rows preserve the input width. Multiline logs and tables remain flexible. Runner Save/Preview/Run/Stop/browser controls now sit above the configuration tabs; the form extends to the popup bottom. The bottom Close button/footer is removed; use the native window close control or Escape. Remove runner is a trash icon with tooltip and confirmation. No custom background fills.


## 0.17.0 runner header controls

Save sits beside the trash icon and appears only while runner settings contain unsaved changes, including passwords, module/addon selection and an active table edit. It hides after saving or reverting edits. Run/Stop are compact84×36 actions; other text actions remain168×36 and icon actions36×36. Buttons and single-line inputs/dropdowns use5px-radius outlines with native backgrounds retained. No autosave or background fills are added.


## 0.17.0 layout correction

Inputs expand to100% of their available field-row area; companion buttons keep a compact width. The fixed320px field wrapper is removed. Standard/custom addon lists reserve120px of height so folder entries remain visible above their controls. Buttons are smaller:144×32 text actions,72×32 compact actions and32×32 icons. Corner rounding delegates to the native border rather than adding a custom outline. No custom backgrounds are introduced. Runtime visual validation remains pending.


## 0.17.0 inline runner controls

Runner editor Save/Run/Stop/Browser/Preview/Trash are tooltip-equipped icons on the runner-name line. Save remains hidden until edits exist. Buttons show a hand cursor. The runner list keeps Stop/Restart/Log; warning/error tabs are accessible through Log. Project summary/count cards are smaller. Editor messages wrap and use Info/Success/Warning/Error foreground colors, with config paths in tooltips. Native button/input/dropdown painting is clipped to5px corners without additional outlines or background colors. Runtime appearance remains pending validation.


## 0.19.0 scrolling, navigation and dependency workflows

The sidebar now shows only Projects/search; Projects/Settings tabs are removed. New runners find a free loopback HTTP/event port pair in the background, excluding every saved runner assignment. Ports are rechecked on launch and are never reserved. The runner lifecycle icon shows Start for inactive processes and Restart for active ones. Back controls use teal arrows/text and stay on the left.

Project create/edit and dependency pages now share grouped, padded, full-width ScrollForm layouts with a single outer scrolling area. Scrolling uses precise-trackpad movement and eased mouse-wheel steps. Hidden pages are excluded from resize layout, unchanged rows avoid repaint work, and background UI refresh backs off during divider dragging. These are source-level changes; actual IDE scroll/resize performance has not been benchmarked.

Dependencies support requirements files and a list of named packages, optional versions/extras (one per line), such as `requests==2.32.5` or `urllib3>=2,<3`. Find on PyPI looks up an **exact package name**, shows latest release details and adds it to the install list. This is not arbitrary full-text search: [PyPI no longer supports pip search](https://pip.pypa.io/en/stable/cli/pip_search/). Lookup uses the [official PyPI JSON API](https://docs.pypi.org/api/json/). Install packages uses the same selected-virtualenv guards and cancellation/output as requirements installation. Finding a package does not install it. No actual package lookup, installation or live port probe was run during development.


## 0.19.0 diagnostics clarity and Odoo Shell

The existing right-side Diagnostics page now shows runner/process state and retained warning/error counts. Warnings and Errors each have a line table with multi-row selection, source/line metadata and a full-message preview below. Copy selected and Copy filtered work on the active tab; table Cmd/Ctrl+C and right-click copy actions are supported. The Log tab can copy selected text or its entire filtered view. Clipboard contents are changed only on an explicit copy action. Filters match literal text in messages/sources; clear empty-state text distinguishes no captured output from no matches. Live updates can be paused while capture continues; retained history is still bounded and counts refer to lines, not unique incidents. Capture/file polling remains100ms, with incremental normal log updates.

Each runner has an **Odoo Shell** terminal icon. It opens an interactive terminal inside the popup’s existing right panel, using the saved interpreter, source, generated `odoo-shell.conf` and one effective database. If the database is blank/multiple, a prompt asks for one database without changing the saved runner. Normal init/update/test launch arguments (and equivalent advanced config entries) are excluded. Shell uses `--shell-interface=python` and `--no-http`; it is a separate Odoo/database process, not attachment to the HTTP worker. Reopening the icon returns to that runner’s retained session; Back stops the shell session before returning to the runners; reopening starts a fresh session. Closing the popup disposes all its embedded terminal sessions. The Terminal plugin must be enabled. Initial support is POSIX/macOS/Linux; Windows is rejected with an explanatory message.

The shell can read/change business records. Odoo normally rolls back its transaction on exit unless you explicitly commit; see the [official shell reference](https://www.odoo.com/documentation/20.0/developer/reference/cli.html). Exit Odoo Shell with Ctrl+D (returning to the underlying local shell); runner Stop controls the HTTP process, not this terminal session. Close shells before changing their virtualenv packages. Shell config files follow the existing private-file/password behavior and remain on disk. Actual shell/clipboard/diagnostics interactions were not run during development.


## 0.20.0 embedded Odoo Shell

Odoo Shell now occupies the existing right panel, with a left-side Runners back button and project/database context. It uses the bundled terminal PTY engine directly, without opening a Terminal tool-window tab. The shell Back action stops its session before returning to runners. Reopening starts a fresh session; closing the popup also ends its sessions. Native terminal theme colors are retained. Compilation/package verification only; interactive IDE behavior remains pending user validation.


## 0.21.0 database browser

Each runner has a database icon opening **the existing right panel**. It uses the saved runner host, port, role, Password Safe password and optional advanced `db_sslmode`; the launch database overrides configured `db_name`. Blank/multiple database selections prompt for one name. The HTTP runner need not be running.

- Search tables by schema/name; sort table names, estimated row counts and storage columns. Storage shows Data (including free-space/visibility maps), TOAST overflow including its indexes, main-table Indexes and Total, plus overall database size. Statistics row estimates can be stale/unknown. Partition sizes are shown per relation; parent size is not a sum of its partitions. Refresh updates figures. Database size can include objects not listed.
- Open a table to browse 100 records per page. Choose a column (or all columns), enter a literal case-insensitive contains filter and press Search/Enter. This runs against the database, not just the loaded page. Shift+scroll moves across columns; select a row and use Edit or double-click it to view its full loaded fields.
- Record editing opens a themed record form. SQL NULL has its own checkbox. Only changed fields are sent on explicit **Save record**, and the update commits one row in one transaction. This is direct PostgreSQL editing: it bypasses Odoo validation, computed fields, access rules and application cache invalidation. Refresh Odoo as appropriate; this browser does not perform ORM writes.
- A primary key and the loaded row-version (`xmin`) must still match; stale or removed rows are rejected and rolled back. No automatic write retries. Primary keys, generated/identity columns, binary/system-reference types and columns lacking UPDATE permission are read-only. Tables without primary keys are view/search only. Values longer than 100,000 characters are truncated for display and make the entire record read-only.
- Uses `psycopg2` from the selected project Python environment. If absent, install the project's Odoo PostgreSQL requirements using Dependencies; nothing is installed automatically. Each request runs in a background Python process with a short-lived database connection, connection/query/lock timeouts and a 16 MiB response limit. Passwords are sent via stdin, never command-line arguments or generated files. Leaving the database panel cancels outstanding reads; closing the popup also cleans up the request process.

Table enumeration is capped at 20,000 and pagination offsets at 10,000,000. Large unindexed/all-column searches may time out. Tables are ordinary/partitioned user tables; views and foreign tables are not included. Database role permissions still apply. PostgreSQL 12+ metadata is required. No insert/delete/table-DDL actions are included.

Implementation references: [PostgreSQL storage functions](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-DBSIZE), [row-version system columns](https://www.postgresql.org/docs/current/ddl-system-columns.html), and [psycopg2 identifier composition](https://www.psycopg.org/docs/sql.html).

Java compilation/package and Python source syntax compilation passed. No database connections, record writes, dependency installs, automated suites or IDE/browser interaction flows were performed; database and UI behavior await user validation. Existing terminal/runner deprecation warnings remain.


## 0.22.0 table coverage, technical names and table UI

The browser now enumerates ordinary tables, partitioned parents/partitions, views, materialized views and foreign tables across user schemas, with no fixed table-count cutoff. The **System objects** checkbox additionally includes PostgreSQL catalogs, information schema and TOAST relations. Other sessions’ temporary schemas stay excluded. Object enumeration does not imply SELECT/UPDATE permission. Queries still have time/response limits: an oversized or timed-out enumeration reports an error rather than silently claiming a complete list.

The list separates **Name**, **Technical name** (`schema.table`), **Odoo model (inferred)**, **Schema**, **Type**, estimated records and four storage columns. Optional `public.ir_model` metadata supplies readable/model names using the conventional dots-to-underscores match. This is an inference: custom `_table` overrides cannot be resolved authoritatively from that metadata. Missing metadata/permissions do not suppress SQL objects; comments/SQL names provide fallbacks. SQL technical names are authoritative.

Search covers readable names, qualified SQL names, inferred model names, schema and type, with case-insensitive literal matching. Schema/type selectors combine with search; the footer shows visible/loaded counts and a no-matches message. The list has padded36px rows, stronger native header typography, resizable columns, teal monospaced technical names and a selected-table storage breakdown. No custom background fills. Shift+scroll reveals additional columns.

Views, materialized views, foreign tables and system objects are read-only in this browser. Their records can still be searched, subject to PostgreSQL permissions and source availability. Objects without a stable key do not promise stable ordering across pages. Storage reflects local relation files, not remote foreign-table storage or partition sums.

Reviewed the [PostgreSQL relation catalog](https://www.postgresql.org/docs/current/catalog-pg-class.html) for relation kinds. Java/packaging and Python syntax compilation passed; live table counts, model matching and IDE rendering were not tested against a running database. Existing deprecation warnings remain.


## 0.23.0 runner-scoped Performance tab

A **Performance** tab follows Server configuration in the runner editor. It samples saved runner settings, not unsaved config fields. Four foreground-only graphs retain up to120 samples in a four-minute time window:

- **Runner CPU** and **Runner RSS memory**: selected HTTP runner process and live descendants (Odoo workers), every2seconds. CPU is CPU-time delta divided by elapsed time:100% means one core and multicore workers can exceed100%. This is never host-wide CPU. New/short-lived/exited workers can be missed between samples. RSS sums resident pages and can double-count shared pages; it is not unique/private memory. RSS uses local POSIX `ps`; Windows, unavailable OS information or more than512 processes show RSS unavailable. Process-tree enumeration capped4096 descendants. The PID/process count identifies the scope. Stopped runners show a stopped gap, not fabricated usage.
- **Database transactions/s** and **connections**: selected single database only, queried every6seconds using saved connection settings and selected project psycopg2. Database size, active/idle/idle-in-transaction clients, lock waits, cumulative deadlocks and interval cache-hit ratio appear above the graphs. Rates warm up after two valid samples and restart after stats reset; cache ratio shows N/A when no block activity occurred. Hidden session states are disclosed when role permissions limit visibility.

Use **Choose database** when config has multiple/no databases, or to temporarily choose another one without editing the runner. Database metrics include other clients using that same database; they do not claim attribution exclusively to this Odoo process. Monitor connections are excluded from displayed connection/state counts. PostgreSQL transaction counters may include this monitor's small transaction overhead. Cumulative stats can lag actual activity. No cluster/global metrics are displayed.

Sampling runs outside the Swing UI thread with bounded history and no overlapping requests of the same kind. Hiding/leaving the tab stops its timer, cancels outstanding database/`ps` work and suppresses stale updates; reopening starts a fresh graph history. Restarting the runner warms up its CPU baseline. Failures appear as unavailable/gaps and a readable status. Native backgrounds retained.

Implementation uses the [PostgreSQL per-database activity/statistics views](https://www.postgresql.org/docs/current/monitoring-stats.html). Java/packaging and Python syntax compilation passed; no actual Odoo/process/DB sampling, automated suites or IDE flows were run.
