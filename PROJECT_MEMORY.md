# Odoo Runner project memory

## 2026-10-24 — Initial plugin

- Standalone folder `odoo-runner-plugin/`; Django platform and existing Git plugins unchanged.
- User model: select Odoo projects/versions; each has multiple named runners, independent ports and UI-generated config files.
- Project-local persistent service stores source/interpreter definitions and runner settings. Three explicit password options use Password Safe; generated configs necessarily contain passwords.
- Swing tool window provides project/runner selectors, common settings, advanced options, masked preview, start/stop and browser opening. Native Run console captures output; project disposal requests termination.
- Initial target build 232.10072.31–232.*, matching existing workspace plugins. Preferred PyCharm version clarification remains unanswered; compatibility is an assumption.
- Java compilation and ZIP packaging succeeded against local build-232 SDK with Java 17 target. No suites or browser/IDE runtime tests run, per user instructions. Runtime behavior remains unverified. See README for initial limitations.
- Parent AGENTS.md references `base/docs/PROJECT_MEMORY.md`, which is absent in this checkout. No foundation changes made.

## 2026-10-24 — 0.2.0 toolbar popup and configuration views

- User requested Odoo logo/top-right access, popup UI with Projects/Settings, project list/form views showing running ports, and distinct odoo-bin launch/test and odoo.conf configuration.
- Replaced tool-window registration with a project-aware toolbar/Tools action and one resizable modeless DialogWrapper per open IDE project. Both New UI MainToolbarRight and classic NavBarToolBarOthers groups registered; Odoo-style SVG wordmark and plugin icon added as code-native vector resources.
- Projects list displays saved source projects with runner/running counts and configured ports of live plugin-managed processes, refreshing each second only while popup is open. A live process is not a readiness check; event port may be unused with workers=0. Clicking opens the project form and runner selector.
- Launch form persists database override, install/update modules, test tags/file, test-enable, stop-after-init, dev features and without-demo modules. Command arguments are passed directly to Python, without a shell. User must choose options supported by the source version; demo-data semantics vary.
- Server form retains existing common fields and Password Safe identities, replaces free-text advanced input with editable key/value table, and migrates existing advanced entries on load. Defaults under Settings apply to new projects/runners. Project path editor includes file/folder browsing. Unsaved runner navigation/close prompts; timer is disposed with popup; closing popup leaves runners active.
- Plugin ID, existing definitions and build-232 compatibility retained. Build script packages 0.2.0. SDK compilation/packaging succeeded; deprecated API note remains. No suites, browser/IDE flows, actual Odoo/test launches or credentials operations run. All runtime acceptance, including toolbar/popup appearance, remains unverified.
- Foundation/platform files remain unchanged; base/docs/PROJECT_MEMORY.md remains absent in this checkout.


## 2026-10-24 — 0.3.0 inline setup and virtual environments

- Add/Edit project now uses a setup card inside the existing popup instead of a separate project modal. Standard path pickers remain chooser windows. Save/Cancel and unsaved-project-close checks are integrated with popup navigation.
- Selecting/typing source automatically schedules asynchronous bounded environment discovery: source itself, common `.venv`/`venv`/`env`/`.env` folders, up to 200 immediate source children, and common environment siblings. Requires pyvenv.cfg plus executable bin/python, bin/python3 or Scripts/python.exe. New source selects its first detected environment; editing retains saved interpreter; manual choices and stale async results are protected.
- Existing interpreter dropdown supports detected paths plus custom browse/typed executable. New environment mode has a base Python selection from PATH/default/common system directories and a custom target (default source/.venv).
- Explicit Create virtualenv invokes selected Python with `-m venv` via direct arguments on a SwingWorker; it atomically creates only a previously absent target with an existing parent. Three-minute timeout and explicit cancellation; process-tree termination is requested and partial files are retained. Success selects the new interpreter. Requirements installation is intentionally not automatic. Creation runs only on the user's later explicit UI action; none executed during development.
- Settings/definitions/plugin ID and build-232 compatibility retained. Artifact 0.3.0; Java17 compilation/packaging against SDK232 succeeded, with existing deprecation note. No suites, browser/IDE flows, runtime discovery/venv creation, dependency installs or actual Odoo launches run. Runtime acceptance remains pending user validation.


## 2026-10-24 — 0.4.0 runner list and row actions

- User requested a runner list with Stop/Rerun/Restart/Settings per runner. Opening a project now shows scrollable runner rows with name, saved HTTP/event ports, effective database and live process status; removed visible runner dropdown from settings. Empty projects show an Add runner prompt.
- Row buttons are actual keyboard-focusable Swing controls. Settings opens the existing odoo-bin and odoo.conf forms inside the same popup; Back returns to runners. Add runner opens its settings, and removal returns to the list. Existing project setup/environment support preserved.
- Extracted saved-definition command/config/launch operations into RunnerEngine; row actions do not depend on whichever runner is selected in the hidden editor. Password Safe IDs and generated config locations unchanged.
- Workspace service coordinates lifecycle: Rerun starts stopped runners or stops/waits/relaunches active ones. Restart requires an active runner. A project-owned pending token prevents duplicate restart actions; a pooled worker waits at most 15 seconds for the old process. Start is revalidated on the UI thread after exit, with port checks repeated. Timeout/start failures show in the row; Stop cancels queued replacement. Closing popup does not cancel queued work; project disposal does. Editing/removal blocked for active or pending-restart runners.
- Version 0.4.0 keeps plugin ID/build232 compatibility and definitions. Java17 compilation/ZIP packaging succeeded against local SDK232; existing deprecated API note remains. No automated suites, browser/IDE flows or actual Odoo lifecycle operations were run. Runtime row UI and stop/rerun/restart acceptance remains unverified.


## 2026-10-24 — 0.5.0 adjustable project/runner split

- User requested Projects tab with projects on the left (30%), runners on the right (70%), adjustable divider, and independently scrollable lists with hidden scrollbars.
- Replaced full-width project table with persistent compact ProjectListPanel sidebar showing project name, runner/running counts, active HTTP/event port pairs, and source tooltip. Selecting a project updates runner/settings/setup cards on the right; unsaved runner/setup navigation checks remain enforced, including blocking navigation during venv creation.
- JSplitPane uses initial 30/70 ratio, minimum usable widths and a draggable divider. Ratio is stored in project-local workspace state. Right-hand project setup and runner forms remain in the existing popup; sidebar stays visible.
- HiddenScrollPane uses normal Swing scroll models with zero-size scrollbar chrome, preserving wheel/trackpad and standard keyboard scrolling. Runner rows track viewport width; row actions now sit below labels to fit the narrower right panel. Scroll positions/focused row buttons remain stable during normal refresh.
- Build 0.5.0 compiled and packaged against SDK232/Java17; existing deprecated API note remains. No suites, browser/IDE flows or live UI checks performed, per user instruction. Actual layout, resizing/divider persistence and hidden-scrollbar scrolling remain unverified runtime acceptance.


## 2026-10-24 — 0.6.0 addon roots, module picker and deployment UI

- User supplied Odoo20 deployment docs and requested multiple standard/custom addon folders, available-port selection, UI deployment controls and install/update module selection from the addon roots.
- Separate ordered multi-folder sections persist standard roots via existing addons field and custom roots via new customAddons field. RunnerEngine merges both into Odoo addons_path, deduplicating in standard-first/custom-second order. New runners infer source/odoo/addons and source/addons when those folders exist. Existing addon text migrates into the standard list; save resolves relative legacy entries against source and validates directories.
- Inline module picker scans immediate children of current UI folder lists asynchronously for __manifest__.py/__openerp__.py, without reading/evaluating Python or connecting to a database. Search and checkbox selection populate install/update launch arguments. Missing previously selected names are retained/labeled; duplicate names take the first root; failed scans disable Apply. This is source discovery, not installed-state or installability verification.
- HTTP/event editable port dropdowns provide an explicit background Find available action probing at most500 localhost TCP ports and offering up to40 results. Other saved assignments/current other port are excluded; current port stays selected even if unavailable. No reservation; existing launch checks remain authoritative. No port probes were executed during development.
- Dedicated deployment fields cover dbfilter, http_interface, db_maxconn, max_cron_threads, memory/time/request limits, logging, proxy_mode, list_db and db_sslmode. Stored in the existing advanced-options field, migrating known rows into form fields on load and preventing duplicate values on save. Remaining arbitrary options stay in table. Controls do not deploy reverse proxy/TLS/PostgreSQL.
- Exact English Odoo20 deploy URL timed out; corresponding official translated deployment/CLI search content was consulted. No claim of complete version-specific option coverage or Odoo20 runtime verification.
- Build 0.6.0 retains ID/SDK232 compatibility. Java17 compile/packaging succeeded (existing deprecated API note). No suites, browser/IDE flows, module scans, port probes or actual Odoo/venv operations executed. Runtime acceptance of all new UI remains pending.


## 2026-10-24 — 0.7.0 grouped forms, scrolling and project actions

- User requested an empty Settings tab pending future planning, corrected conf/odoo-bin scrolling, configuration grouped using Odoo website references, PostgreSQL executable path, left-side Edit/Remove project actions, and a larger popup.
- Global Settings controls removed; tab now only contains the planning placeholder. Existing persisted internal defaults are retained for compatibility. Runner configuration remains under each runner's Settings action.
- odoo.conf now has titled groups: Runner, Database, Addons/directories (data and pg tools), HTTP/ports/proxy, Workers/resource limits, Logs, SMTP, Master password/credentials, Additional options. Launch form has source paths, database/modules, tests, development/startup and notes. Persistence/Password Safe IDs remain unchanged.
- New grouped fields include pg_path, db_template, log_db and SMTP server/port/user/SSL/from-address, stored alongside existing options in r.extra; known advanced rows migrate into the grouped fields on load. pg_path is validated as an existing absolute directory; it configures PostgreSQL dump/restore executable lookup, not a database server installation. Official Odoo20 CLI website search content consulted for pg_path/logging/SSL/config mapping; selected source version still determines option support.
- Added ScrollForm with viewport-width tracking and natural-height sections. Removed oversized/nested form wrappers and nested advanced table scroll pane. HiddenScrollPane now processes precise wheel deltas explicitly instead of relying on hidden scrollbar geometry; nested addon lists forward wheel events to the containing form when at their limits. Existing keyboard scroll models retained. This source change addresses the reported behavior; runtime scroll fix remains unverified.
- Edit/Remove project now reside in the persistent left sidebar, with unsaved setup/runner guards enforced there. Runner header keeps Add runner. Larger initial popup content size up to1280x860 is constrained to usable screen bounds; new dimension key prevents an older small saved size overriding the first new opening.
- Build 0.7.0 compiles/packages against SDK232/Java17, with existing deprecated API note. No automated suites, browser/IDE flows or runtime operations run, per user instruction. Grouped UI, larger popup and scrolling acceptance remain pending user validation.


## 2026-10-25 — 0.8.0 row icons, click-to-configure and path browser

- User requested project edit/delete icons inside left-panel rows, clickable runner rows opening configuration, right-aligned icon-only lifecycle controls with one Restart instead of Rerun+Restart, colored status, and path chooser search/back/forward/path entry.
- Project sidebar now uses real Swing row components with per-row pencil/trash icon buttons, preserving unsaved navigation guards, stopped-project requirements and definition-removal confirmation. Timer refresh updates labels instead of replacing focused controls.
- Runner row click/Enter/Space opens configuration; separate Settings and Rerun buttons removed. Right-aligned Stop and Restart icon buttons have accessible names/tooltips. Single Restart starts inactive runners or coordinates stop/wait/relaunch for active runners using existing service tokens/timeouts. Status dot/text uses green Active, gray Inactive and amber transitional state; detailed errors/exit codes remain in tooltips.
- New PathBrowser DialogWrapper replaces JFileChooser for folder/file browse actions: back/forward history, up/home, editable path navigation (absolute/relative/tilde), current-folder name/path filter, asynchronous directory reads, multi-folder selection and direct typed file/folder selection. Search is not recursive; results capped at20,000. Existing data directories are not modified by browsing. Chooser paths are revalidated before selection.
- Code-native vector icons added via Icon painting; no generated raster assets or image tools needed.
- Version0.8.0 keeps plugin ID/SDK232 compatibility and saved definitions. Java17 compilation/ZIP packaging succeeded against SDK232 with existing deprecated API note. No automated suites, browser/IDE flows, chooser interactions or actual Odoo operations executed; runtime acceptance remains unverified.


## 2026-10-25 — 0.9.0 runner diagnostics and pip action

- User requested a warnings/errors icon after Restart opening a right diagnostics section, a separate runner log icon, and project-level pip dependency installation into the saved virtualenv.
- Added icon-only issue/log controls and a resizable right diagnostics pane with follow-output toggle. Workspace captures stdout/stderr via ProcessAdapter independently of popup lifetime and follows configured logfile appends with a daemon reader, truncation/rotation handling and bounded catch-up. Filter recognizes level tokens and traceback continuations; counts are lines, not unique incidents. Full log2500/issue1200 lines retained in memory; capture resets per launch and is not persisted.
- Captured output redacts known Password Safe runner values and basic URL username/password text. Native IDE console remains separate; no claim that arbitrary application output is secret-free. No database ir.logging integration or old logfile import. Existing running processes need a restart to attach capture after plugin update.
- Project package icon opens an inline requirements form with selected interpreter, default source/requirements.txt, file browse, explicit Install and Cancel, output/status. Invokes selected Python with -m pip --require-virtualenv install --requirement using direct arguments, no shell. Requires conventional venv marker and stopped project runners; pip/user target overrides constrained. Starts/edits/removal blocked while same-project pip process runs. Work belongs to IDE project and survives popup close; disposal requests termination. No automatic requirements installs or rollback/system dependency provisioning.
- Pip command form checked against official pip install docs. No real pip install, package download, Odoo/logfile interaction, browser/IDE flow or automated suite executed. Java17 compile/ZIP packaging against SDK232 succeeded with existing deprecation note; runtime diagnostics/pip acceptance remains unverified.


## 2026-10-25 — 0.10.0 in-place diagnostics, severity rows and live-update scheduling

- User corrected diagnostics layout: reuse the existing right panel, separate Warnings/Errors tabs with line filtering, improve slow logs and modernize UI without background fills.
- Removed nested diagnostics JSplitPane. Warnings/log icons now open a diagnostics card in the existing projectPages right panel; Back restores runners. Warnings and Errors tabs share structured line data but apply separate severity+literal source/message filters; Log is a third tab. Rows show stable line sequence, severity, source and message, with sorting/selection/tooltips.
- RunnerLogs now retains structured entries and per-stream severity continuation. UI refreshes only when revision changes; batches table updates and appends new log text rather than replacing the document, trims evicted lines and retains Follow control. History remains bounded and launch-local.
- Identified source-level latency causes: independent1s file reader and1s UI refresh plus full-text reset. Changed logfile polling and coalesced visible diagnostics timer to100ms; diagnostics no longer depends on the general1s list refresh. This improves scheduling, not a runtime latency guarantee; partial lines still wait for newline/flush and source buffering remains outside plugin control.
- ModernUi adds consistent input padding/button spacing/table rows. Sidebar selection uses a border accent instead of custom selected background fill; no custom background fills introduced. Status foreground colors retained.
- Build0.10.0 compiles/packages with SDK232/Java17 (existing deprecation note). No suites, browser/IDE flows or live timing/logging tests run, per project instruction. Runtime severity filtering, line scrolling and latency acceptance remain unverified.


## 2026-10-25 — 0.11.0 supplied screenshot design

- User supplied a dark Odoo Runner UI image and asked to try it. Treated it as the new visual direction, superseding the prior no-background-fill preference for this iteration.
- Added a scoped dark navy palette, blue primary actions, rounded outlined cards and vector cube/server badges. No global IDE UIManager defaults modified. Native popup titlebar/window controls stay IDE/OS-owned.
- Projects/Settings navigation moves into the top of the left sidebar. Project search filters names/source paths. Selected project card uses blue accent and retains edit/pip/delete controls, runner counts and active ports.
- Runner view has project summary header, total/active statistic cards, a Runners section, primary Add runner control, inline name/status plus connection details, right-aligned lifecycle/diagnostics controls and a footer hint card matching the reference hierarchy. Existing right-panel configuration, diagnostics and dependency operations retained.
- Version0.11.0 compiles/packages with SDK232/Java17 (existing deprecation note). No automated suites, browser/IDE flows or visual/runtime checks performed. Screenshot similarity, actual light/dark IDE presentation and responsive sizing remain unverified until user validation.


## 2026-10-25 — 0.11.1 restore explicit no-background requirement

- User clarified that the screenshot did not revoke their explicit no-background-color instruction. The 0.11.0 interpretation was incorrect; reference is for layout/hierarchy only.
- Removed all custom surface/input/button/selection background assignments and card/badge fill painting. Removed fixed dark/light text palette; outlines and labels use native IDE theme colors. Kept sidebar navigation/search, summary statistics, rounded outlines, spacing, icons and layout. Native IDE backgrounds/selections remain unchanged. Status foreground indicators retained.
- Version0.11.1 built against SDK232/Java17; no suites or runtime/visual flows run. Runtime appearance remains pending user validation.


## 2026-10-25 — 0.12.0 uniform actions and reference-style forms

- User supplied a runner-settings reference image, requested consistent button sizing and reiterated no background color. Reference is for structure and alignment only; native surfaces retained.
- All plugin text action buttons use168x36; icon-only buttons use36x36, including generated rows and dialog actions. Navigation uses fixed-size wrapping actions; action bars wrap onto additional rows rather than clipping equal-width buttons on narrower panels. Native combo/scroll arrow internals are excluded from text-button sizing.
- Runner settings header now shows breadcrumb/back control, cube badge, runner name/status/connection summary and right-side Remove runner. Form sections have rounded outlines and bold headings, consistent label column widths, and natural-height scrolling. Existing field values/operations unchanged.
- Removed no native theme backgrounds and introduced no custom fills. Verified by source search that Design/ModernUi/ScrollForm contain no setBackground or fill painting. Status icon foreground color remains as requested previously.
- Build0.12.0 Java17/SDK232 compilation and ZIP packaging passed with existing deprecation note. No automated suites, IDE/browser flows or runtime visual checks run. Actual sizing/wrapping and reference-style layout still require user validation.


## 2026-10-25 — 0.13.0 closer teal reference details, no backgrounds

- User reiterated matching reference iconography, spacing and colors, explicitly excluding backgrounds. No background fills introduced; native surfaces/selections remain unchanged.
- Accent outlines/text now use reference teal; Remove runner uses a red outline/text. Added code-native folder, database, code, flask, gear, save, play, terminal, globe and plus icons for actual UI actions and section headings. Text/icon dimensions remain168x36/36x36 from prior request.
- Group cards show left-side outlined icon badges and bold inset headings. Launch source displays separately aligned path/Python/script/working-directory rows with monospaced path labels and edit-project hint. Module/test/development inputs have reference-style helper text. Existing saved data and operations preserved.
- Native popup chrome, theme fonts and responsive wrapping remain IDE-dependent; exact pixel equivalence is not claimed without runtime validation. No suites, IDE/browser flows or screenshot tests executed. Build0.13.0 compiles/packages with SDK232/Java17, existing deprecation note remains.


## 2026-10-25 — 0.14.0 top actions, uniform inputs and no Close footer

- User requested uniform button/input sizes, no bottom Close action, runner actions at top to free configuration space below, and icon-only Remove runner.
- Single-line text/password fields and dropdowns share320x36 sizing; ScrollForm keeps standalone and composite picker fields at that width, including fields with browse/module/port buttons. Text buttons retain168x36 and icon actions36x36. Multiline log/output/table surfaces retain their content sizing.
- Save/preview/run/stop/browser actions and status moved into the top runner header. Configuration tabs consume the remaining center area down to the bottom. Removed RunnerDialog south action panel/Close button; native window close/Escape still uses existing unsaved guards. Remove runner is a36px trash icon with tooltip/accessible name and existing confirmation.
- No custom background fills introduced. Version0.14.0 compiles/packages against SDK232/Java17 with existing deprecation note. No suites, browser/IDE or runtime visual checks run; size/wrapping and layout acceptance remains pending.


## 2026-10-25 — 0.15.0 conditional Save, compact lifecycle and radius5

- User requested Save beside trash only when edited, smaller Run/Stop, and5px corner radius for buttons/inputs/selections.
- Runner Save moved from action bar to summary beside trash. A100ms UI check compares saved snapshot, password edits and an uncommitted advanced-option table cell; Save appears only for edits and hides after save/load or reverting all edits. It does not autosave. Existing stopped-runner save requirement remains.
- Run/Stop text actions now84x36; other text buttons168x36 and icons36x36 retained. Added RoundedOutline painting with5px corner radius for plugin buttons, text/password fields and dropdowns. Focus uses teal outline; primary and destructive accents retained. No custom background fills.
- Build0.15.0 Java17/SDK232 compilation/packaging passed with existing deprecation note. No automated suites, IDE/browser or runtime visual flows run. Conditional-save/rounded-control runtime acceptance pending user validation.


## 2026-10-25 — 0.16.0 flexible field widths and addon-list visibility

- User clarified corner rounding only, without added outlines; inputs should fill100% available field-row width; addon folder list content was clipped/collapsed; buttons should be smaller.
- Removed custom RoundedOutline drawing. RoundedNativeBorder delegates/clips the existing native border at5px radius, adding no new stroke or background. Native roundRect hint retained; no custom surface fills.
- Removed fixed-width FlowLayout input wrappers from ScrollForm. Text/password/dropdown controls now have flexible maximum width and80px minimum,32px height; GridBag/BorderLayout expand fields to available width, with browse/module buttons retaining their own compact width.
- Addon panels reserve120px preferred/112px minimum list area with28px rows, ensuring folder entries are visible above actions. Folder rows have padding and full-path tooltips. Hidden-scrollbar behavior retained.
- Reduced text actions to144x32, compact Run/Stop/reorder actions72x32, icon actions32x32. Version0.16.0 compiles/packages against SDK232/Java17, existing deprecation note. No suites, IDE/browser flows or runtime visual tests run. User screenshot is observed evidence of the old clipping; new layout runtime acceptance remains pending.


## 2026-10-25 — 0.17.0 inline runner icons, compact summary and readable messages

- User screenshot showed header action wrapping/clipped labels, unclear long status and ineffective rounding. Requested icon-only run/stop/browser near save/trash on the runner-name line, hand pointer, actual5px rounding, removal of runner warning icon, compact project counts and semantic message colors.
- Replaced wrapping editor action rows with a single non-wrapping name/status/action header. Save (still dirty-only), Run, Stop, Browser, preview icons and Trash are32px icon actions with tooltips/accessibility names. Name/status use non-wrapping layout. All button hover cursors use HAND_CURSOR.
- Runner list now has only Stop/Restart/Log actions. Log opens existing in-place diagnostics retaining Warnings/Errors tabs. Project summary/count fonts, padding and statistic cards reduced.
- Rounded native control subclasses clip complete native button/text/password/dropdown painting at5px radius, rather than only clipping the border. No additional border strokes/custom background fills added.
- Editor StatusMessage wraps readable short text and uses foreground/icon colors for Info/Success/Warning/Error. Config path moved to tooltip instead of an unreadable long inline path. Existing save/start/stop actions emit semantic status; caught errors also appear in status while retaining error dialogs.
- Version0.17.0 Java17/SDK232 compilation and packaging passed, existing deprecation note. No suites, IDE/browser flows or runtime visual checks run. Actual native-control corner appearance and layout require user validation.


## 2026-10-25 — 0.17.1 teal project and runner names

- Project names in sidebar/summary/breadcrumb and runner names in list/editor use the existing button teal accent (Design.BLUE). No background changes.
- Java17/SDK232 build packaged0.17.1. No automated suites or runtime visual flows run; color appearance pending user validation.


## 2026-10-25 — 0.18.0 performance/source fixes and unified setup/dependencies

- User reported scrolling/divider lag and requested removing sidebar tabs, live available-port allocation for new runners, Start/Restart icon by process state, left-side teal Back controls, dependency package search/install and consistent setup/dependency forms.
- Source inspection found unchanged project rows allocating borders/repainting each second; hidden CardLayout pages participating in resize/layout; coarse72px-style wheel jumps; dirty snapshots running on hidden editor; dependency output serializing even when unchanged. Added state fingerprints, visible-page-only card layout, skip background UI refresh during divider changes, visible-only dirty checks, revision-gated dependency output and unchanged-logfile read fast path. Kept continuous divider resizing; no claim of measured FPS/latency.
- HiddenScrollPane now uses smaller precise-trackpad deltas and16ms eased wheel steps with scroll chaining and blit viewport scrolling; stops animations while hidden. Native surface colors retained.
- Removed Projects/Settings navigation and unreachable Settings placeholder. Sidebar is project search/list only. New runner creation discovers a live free HTTP/event pair in a worker, excludes all saved runner assignments, rechecks saved collisions and guards navigation/unsaved edits before adding; launch still rechecks ports because availability is not reserved.
- Inactive runner lifecycle icon is Play/Start; active uses Restart. Back controls show teal arrow/text and are placed left, including diagnostics, dependencies and project setup.
- Project create/edit now uses matching outlined ScrollForm sections and a single outer hidden-scrollbar view, fixed-height activity area, top back/save and wrapping status. Existing discovery/creation guards retained.
- Dependencies use padded grouped forms with selected virtualenv, requirements path, exact-name PyPI lookup, named package/version list, install/cancel and output. Official PyPI JSON API used only after explicit Find; PyPI no longer supports pip search/full-text search API. Exact lookup returns latest version/summary/Python requirement; users may enter other named versions/extras directly. Named packages pass validated arguments to the existing guarded selected-venv pip process. URLs/options must use requirements workflow rather than direct package entry. No packages installed or PyPI lookup executed during development.
- Build0.18.0 Java17/SDK232 compilation/packaging succeeded, existing deprecated API note. No automated suites, IDE/browser/scroll benchmarks, live port probes or actual pip/Odoo operations run. All runtime/performance acceptance remains pending user validation.


## 2026-10-25 — 0.19.0 clear diagnostics/copy and interactive Odoo Shell

- User asked to plan/improve diagnostics clarity, copy warnings/errors and add a runner Odoo Shell icon.
- Existing right diagnostics card gains process/retained-severity counts, clearer empty states, multi-row warning/error selection, full-message preview, Copy selected/filtered, table keyboard/context-menu copy and log text copying. Search also filters log text; Live updates can pause UI while capture continues. No custom backgrounds. CopyPasteManager writes only on explicit copy action; none executed during development.
- Runner terminal icon opens actual interactive Odoo shell via bundled TerminalToolWindowManager, avoiding piped stdin (Odoo requires TTY for interactive behavior). Saved Python/source/config and effective single database are used; ambiguous database prompts user. No HTTP binding or saved install/update/test launch options; corresponding config entries are stripped. Separate shell.conf preserves private-file behavior. POSIX shell quoting; Windows unsupported initially. Required Terminal plugin dependency/build classpath added.
- Shell opens a new separate database process each click, not an attachment to HTTP runner; lifetime managed in Terminal. No shell/DB commands were executed during development. Official Odoo CLI shell docs consulted for invocation/transaction semantics.
- Build0.19.0 compiles/packages against SDK232/Java17 with existing deprecation note. No suites, browser/IDE flows, actual clipboard interactions or shell launches run. Runtime terminal input, copy/filter/UI clarity acceptance remains pending.


## 2026-10-25 — 0.20.0 shell embedded in existing right panel

- User requested Odoo Shell within the popup instead of the IDE Terminal tool window. Added a right-side shell card with teal runner title, project/database context and left-side Runners back action.
- ShellTerminalWidget and LocalTerminalDirectRunner provide a real interactive PTY directly inside this card. No Terminal tool-window tab is created; private config, saved interpreter and guarded shell invocation retained. Native terminal theme retained, no custom surface colors.
- One session per runner is reused while navigating the popup. Back preserves the session; popup disposal disposes all session lifetimes. Ctrl+D exits Odoo into its underlying local shell. HTTP Stop remains independent.
- SDK232/Java17 compilation/packages0.20.0 succeeded; terminal session API emits a deprecation/removal warning on this SDK. No automated suites, IDE flows or actual shell/database operations run. Interactive focus, input and disposal runtime behavior require user validation. base/docs/PROJECT_MEMORY.md remains absent; standalone plugin memory used.


## 2026-10-25 — 0.20.1 stop shell on Back

- User explicitly requested Back from Odoo Shell terminate that session. Back now disposes the current terminal lifetime and removes its retained widget/session before showing runners. Opening Shell again creates a fresh session. Popup disposal still cleans up all remaining sessions. UI hint and README updated. Session card identifiers now use an increasing counter to avoid reuse collisions after removal.
- Compiled/packaged against SDK232/Java17; existing deprecation warnings remain. No automated suites, IDE flows or shell/database operations run; actual process termination awaits user runtime validation.


## 2026-10-25 — 0.21.0 runner database browser and record edits

- User requested a DB icon per runner, table/record browsing and editing, table search, record search by column and storage usage. New DatabaseBrowserPanel occupies the existing right card, with left Runners/ Tables Back actions, native surfaces, teal heading and standard controls. Record view/edit uses a modal form with explicit Save (no inline autosave).
- DatabaseBridge invokes selected project Python/psycopg2 using an embedded Python resource. Saved effective host/port/user/SSL/PasswordSafe password and single effective database are used; ambiguous DB prompts user. Secrets use stdin only, no credential/config files or command-line password. No added Java driver/downloads/automatic dependency installation.
- Table/schema literal search, numeric storage sorting, estimated rows, per-relation data/TOAST/index/total sizes and whole DB size. Capped20k tables, refresh-on-request; partition parent sizes are not aggregated. Records use100-row server pages, literal case-insensitive selected-column/all-column filter, stable primary-key ordering where possible. Offset cap10m, values capped100kchars, response16MiB, bounded connect/query/lock/process times. Large searches can time out.
- Explicit single-row transaction updates only changed editable fields, uses psycopg2 identifiers/value parameters, primary key plus original xmin guard; conflict/non-single-row updates roll back. No automatic retries. Primary keys/generated/identity/binary/system-reference fields/no UPDATE permission are protected. No-primary-key/truncated rows read-only. SQL NULL separate checkbox. Direct SQL bypasses Odoo ORM validation/recompute/access rules/cache invalidation; UI and README state this. No insert/delete/DDL implemented. Ordinary/partitioned tables only; PostgreSQL12+ catalog assumptions.
- Reads run off EDT; table metadata applied as one model event; panel hide/back/popup disposal cancels request processes and suppresses stale callbacks. Each request closes its own DB connection. Record form blocks navigation while save is pending.
- Official PostgreSQL storage/xmin docs and psycopg2 SQL-composition docs checked. SDK232/Java17 compilation/package0.21.0 and Python source syntax compilation passed; existing terminal/runner deprecation warnings remain. No automated suites, actual DB connections/reads/writes, dependency installs or IDE/browser flows run. Runtime editing, connection permissions, UI layout and storage/filter behavior remain unverified. Framework memory path base/docs/PROJECT_MEMORY.md still absent; framework untouched.


## 2026-10-25 — 0.22.0 complete table-kind coverage and technical-name UI

- User requested checking all tables show, technical names/search and improved table UI. Source audit found old r/p-only filter and20k cutoff. Enumeration now includes ordinary/partitioned tables/partitions, views/materialized views/foreign tables across user schemas without a fixed count cap. System toggle adds catalogs/information schema/TOAST; other sessions’ temporary schemas excluded. Request timeout/16MiB limits still report explicit failure, not a partial successful list. No live DB count verified.
- List separates readable Name, qualified SQL Technical name, inferred Odoo model, schema/type, estimated rows and data/TOAST/index/total bytes. Optional public.ir_model lookup under savepoint gives conventional model-to-table inferred matches and translated display labels; custom _table overrides unresolved and explicitly disclosed in header/tooltip/docs. Missing metadata does not hide SQL objects. Comments/table names fallback.
- Literal case-insensitive search across names/model/schema/type, schema/type filters, visible/loaded counts/no-match feedback, padded36px rows, bold native headers, teal monospaced technical names, resizable horizontal columns and selected-object storage footer. No custom surfaces. Search debounced150ms; metadata still applied as one model event.
- Read/search supports newly included object kinds without assuming xmin/tableoid/ctid on views or foreign tables. All non-user-table/system objects are protected from update in backend; editor/status explains read-only state. Views/foreign objects without primary keys have no guaranteed page order.
- Official PostgreSQL pg_class relation-kind docs reviewed. SDK232/Java17 build0.22.0 and Python syntax compilation passed; existing terminal/runner deprecation warnings remain. No automated suites, live database operations, IDE/browser flows or live completeness checks run. Source verification only, runtime acceptance remains pending.


## 2026-10-25 — 0.23.0 selected-runner/selected-database performance

- User requested a tab after server configuration for CPU graphs and particular DB status, explicitly not overall performance. Added RunnerPerformancePanel as the third runner editor tab, with CPU, summed RSS, DB transactions/sec and client connections time graphs plus scoped DB status. Uses saved settings, optional temporary single DB selection. Native backgrounds/teal traces; no chart fills.
- Java ProcessHandle CPU durations include selected managed runner and live descendants; sampled2s, one-core100% (can exceed100%), unavailable/stopped gaps. Local POSIX ps supplies summed RSS (shared pages double-count); Windows/>512-process tree/missing OS info -> unavailable. Descendants capped4096. Newly created/exited/short-lived workers between samples can be missed. CPU baselines warm after restart/first sample. No host/global CPU metrics.
- Extended Python bridge with read-only metrics action restricted to pg_stat_database WHERE datname=current_database() and pg_stat_activity same DB/client backends. Every6s: commits/rollbacks for rates, reads/hits interval ratio, db size, active/idle/idle-tx, lock waits, deadlocks and hidden state count. Monitor connections excluded from activity counts; transaction counters still include observer overhead. DB metrics include all clients of chosen DB, never claim exclusive runner attribution. Stats-reset/time/negative-counter guards; no fake cachehits on no activity. Permissions/missing dependencies/errors disclosed. Official PostgreSQL monitoring docs reviewed.
- Four-minute chart time axis/up to120 points, no external chart dependency. Swing workers collect off EDT, separate bounded polling/no per-kind overlap, hierarchy visibility start/stop, request generation guards and popup disposal cleanup. Hiding/reopening resets graph history; stopped runner can still show chosen DB activity.
- SDK232/Java17 compilation/package0.23.0 and Python syntax compilation passed, existing terminal/runner deprecation warnings remain. No suites, IDE flows, actual process sampling or DB operations executed. Runtime graph visibility/metrics/permissions/performance acceptance remains pending user verification.


## 2026-10-25 — 0.24.0 Marketplace preparation, MIT and official compatibility check

- User asked Marketplace compatibility check and full plugin docs, author Oewer Solutions/contact oewersolutions@gmail.com. User explicitly chose MIT and deferred public source repository setup; vendor website initially requested and remains unset. No source/vendor URLs invented and no upload/account/email/publication action taken.
- Added MIT LICENSE (copyright2026 Oewer Solutions), vendor/email, detailed English descriptor/change notes, original40px transparent light/dark Marketplace database/play SVG icons. Kept requested toolbar Odoo wordmark; actual permitted use/name uniqueness remains owner's submission review item, not claimed cleared by disclaimer. Python module/Terminal dependencies explicit; restart required. Stable plugin ID/build232 bounds retained.
- Replaced obsolete contradictory README with current user/build/limits/support guide, archived prior notes under docs/DEVELOPMENT_HISTORY.md. Added CHANGELOG, PRIVACY, SECURITY, THIRD_PARTY_NOTICES, CONTRIBUTING, Marketplace listing/readiness docs and preserved verifier verdict/dependency evidence. Privacy describes local settings, PasswordSafe, plaintext generated configs, retained files, PyPI/pip/remote DB interactions and absence of vendor telemetry. No business data/credentials in docs.
- Builder reads version from plugin.xml, packages MIT license in main JAR and userdocs in ZIP (not IDE libraries), release0.24.0. Added stdlib scripts/verify_release.py checks for artifact metadata/version/deps/icons/license/Java17/Python syntax with separate pending publication URL status and optional strict Marketplace mode. Local checks pass with two pending URLs.
- Downloaded official JetBrains Plugin Verifier1.411 with authorized network access. Initial network-dependent run failed on optional dependency resolution in restricted sandbox; reran documented offline mode against local PC-232.10072.31. First completed verdict compatible but identified internal/scheduled-removal AbstractTerminalRunner.openSessionInDirectory and deprecated ExecutionManager.getContentManager/java.net.URL constructor. Replaced with public startShellTerminalWidget/ShellStartupOptions, RunContentManager.getInstance and URI.toURL.
- Final official offline verification on PC-232.10072.31 is Compatible with no compatibility/structure/internal/experimental/deprecated/removal reports. Required platform/Python/Terminal dependencies resolved; absent other-product optional transitive modules in Python plugin are not failures. Clean Java17 compile and Python syntax check. This is binary/source/package evidence, not live IDE functionality.
- Compatibility still declared232.10072.31–232.*; actual tested baseline Community232.10072.31 only. Installed PY-253.28294.256 outside declared range and not supported/claimed. No wider branch upgrade, production DB operations, installations or IDE UI tests performed. Remaining public repo/vendor URLs, branding rights/name uniqueness, fresh runtime smoke/screenshots, vendor/account agreement/trader choices and Marketplace review documented. Framework memory path absent as before; standalone framework untouched.
