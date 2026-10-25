# Marketplace readiness — 0.24.0

**Status: clean binary compatibility on the checked IDE; not yet ready for public submission.**

Author: **Oewer Solutions**. Contact: **oewersolutions@gmail.com**. License: **MIT**, explicitly chosen by the owner. The owner deferred public repository/website setup; no URLs were fabricated and nothing was uploaded/published.

## Verified evidence

| Check | Result |
| --- | --- |
| Java build | Passed against local PyCharm Community PC-232.10072.31 SDK, `--release 17` |
| Official checker | JetBrains Plugin Verifier1.411, downloaded from the official GitHub release |
| Checked plugin | `dev.odoorunner`, version0.24.0 |
| Checked IDE | **PC-232.10072.31** (PyCharm Community2023.2 baseline) |
| Mode | Offline, local SDK/dependencies/runtime; no compatibility findings muted |
| Verdict | **Compatible** |
| Compatibility/structure/API reports | No compatibility, plugin-structure, internal, experimental, deprecated or scheduled-for-removal reports generated in final run |
| Required dependencies | Platform/PythonCore/Terminal resolved from target SDK |
| Optional transitive dependencies | Missing other-product modules in bundled Python plugin are recorded as optional in dependencies report, not compatibility failures |
| ZIP checks | Descriptor/version, vendor/email, dependency declarations,40px SVG icons, MIT copyright/license, Java17 class headers, Python syntax and documentation packaging checked locally |
| Newer IDEs | Not declared compatible. Installed local IDE is PY-253.28294.256, outside build232 bounds; no claim it can install this release |
| Runtime/UI/database tests | Not performed in this release task; previous functionality remains pending manual smoke validation |

Final official evidence: [verdict](verification/0.24.0-PC-232.10072.31-verification-verdict.txt) and [dependency graph](verification/0.24.0-PC-232.10072.31-dependencies.txt). Full local HTML/report output is under `build/verification-0.24.0-final/` (not committed/shipped).

The first offline check flagged an internal/scheduled-for-removal terminal startup API and two deprecated calls. Those were replaced with supported startup, run-content and URI APIs; the final check is clean. An earlier online attempt failed fetching optional dependencies because sandbox networking was unavailable; that failed attempt is not counted as a compatibility result.

The declaration `232.10072.31–232.*` is intentionally retained. Only the stated baseline build/Community product was checked; Professional, later232 patch builds, newer IDE branches and all operating systems were not independently verified. Do not broaden the bounds until the corresponding binaries and runtime behavior have been checked.

## Pending before submission

1. **Publish the complete MIT source repository**, including LICENSE and documentation, and supply its real public URL. JetBrains requires a source link for open-source licenses. The owner will set this later.
2. **Supply a valid public vendor website** (the repository/support page can be used if appropriate), add it to vendor metadata and the Marketplace vendor profile, and verify all public source/support/privacy links resolve. Email is already present. The owner deferred the website.
3. **Confirm permission for Odoo trademark/wordmark usage** or adopt permitted original naming/assets. The Marketplace manager icon is an original database/play SVG, but the existing toolbar Odoo wordmark and Odoo product name are still used. An independence disclaimer does not itself grant trademark rights. Name uniqueness on Marketplace is not established.
4. Run the documented manual smoke checks in the declared IDE against disposable/local data. The terminal startup API changed in this release; input/startup/disposal need runtime confirmation. Capture real, sanitized release screenshots.
5. Create/select the Oewer Solutions vendor profile, accept JetBrains' developer agreement, select MIT and provide source URL, confirm trader/non-trader status, then upload the candidate ZIP. These account/legal decisions remain with the owner. No account changes were made.
6. Review Marketplace's own verifier/reviewer results. Local verification is evidence, not guaranteed approval. Source publication and a privacy URL also need to match the shipped behavior.

No vendor website, repository, account, screenshots or permission statement has been invented. Do not submit placeholder URLs or the archived design mockups as screenshots.

## Reproduce

Build, then check the final archive:

```sh
python3 build.py --ide /path/to/pycharm-232 --jdk /path/to/jdk
python3 scripts/verify_release.py dist/odoo-runner-0.24.0.zip
java -Dplugin.verifier.home.dir=/tmp/odoo-verifier \
  -jar /path/to/verifier-cli-1.411-all.jar check-plugin \
  dist/odoo-runner-0.24.0.zip /path/to/pycharm-232 \
  -runtime-dir /path/to/jdk -offline \
  -verification-reports-dir build/verification-0.24.0
```

Use offline mode only when the IDE's required dependencies are already present. Once URLs are supplied, `python3 scripts/verify_release.py dist/odoo-runner-0.24.0.zip --marketplace --source-url https://your-real-public-repository` also fails on missing publication URLs. This local script checks URL shape, not reachability, ownership or legal permission.

## Primary requirements reviewed

- [Marketplace approval guidelines](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html): vendor details, branding, functionality, API compatibility, privacy and review.
- [New plugin upload](https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-plugin.html): vendor profile, agreement, license, source URL and upload fields.
- [Plugin logo specification](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html): SVG40×40, padding and META-INF placement.
- [Official Plugin Verifier](https://github.com/JetBrains/intellij-plugin-verifier): binary checks, report types and offline mode.
