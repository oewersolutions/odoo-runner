# Contributing

Odoo Runner is MIT licensed. Maintainer: Oewer Solutions, oewersolutions@gmail.com. A public repository URL must be supplied before the MIT Marketplace submission; no repository has been invented by these documents.

Read PROJECT_MEMORY.md before changing behavior. Keep generated configs/credentials, local IDE paths, build outputs and user data out of contributions. Preserve the stable plugin ID for updates, keep native IDE backgrounds and distinguish source checks from runtime verification.

## Build

Use Python3, a local PyCharm2023.2/build232 SDK and a JDK containing javac:

```sh
python3 build.py --ide /path/to/pycharm-2023.2 --jdk /path/to/jdk
```

The JDK must support `--release 17`. The builder uses IDE/Terminal libraries only as a compile classpath; they must not be copied into the plugin ZIP. plugin.xml supplies the version.

## Verify release compatibility

Download the official JetBrains Plugin Verifier from the release link in its upstream documentation. Run it against the exact candidate ZIP and real target IDE installation, with the target Terminal/Python plugins present. Do not broaden compatibility based only on successful compilation.

```sh
java -jar /path/to/verifier-all.jar check-plugin \
  dist/odoo-runner-0.24.0.zip /path/to/pycharm-2023.2 \
  -runtime-dir /path/to/jdk \
  -verification-reports-dir /path/to/reports
```

Use the CLI help for version-specific options. Inspect compatibility, internal/experimental/deprecated API and plugin-structure reports, not just process exit status. A successful binary check does not exercise startup, UI or database operations. Keep the evidence and exact tool/IDE versions in docs/MARKETPLACE_READINESS.md.

Runtime smoke checks before publication should cover install/open/close, project/venv/runner operations, logs, shell shutdown on Back, database read/edit conflict behavior on a disposable database, and visibility-scoped performance sampling. Never use production data for destructive checks. Publishing/uploading and Marketplace account agreements are separate actions performed by the maintainer.
