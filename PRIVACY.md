# Privacy and data handling

Effective for Odoo Runner 0.24.0. Publisher: Oewer Solutions. Contact: oewersolutions@gmail.com.

## Processing on your computer

The plugin processes source/interpreter paths, project and runner configuration, process output, database connection settings, database metadata/records and process/database statistics to provide features you open or start. It does not send those data to Oewer Solutions and contains no vendor analytics/telemetry service.

Project/runner definitions are saved in the IDE's workspace storage. Dedicated password fields are stored through JetBrains Password Safe; the exact storage/persistence is determined by your IDE/password-safe settings. Graph samples and bounded captured logs are held in memory. Configured Odoo log files and generated files can persist on disk.

Generated `.odoo-runner/<runner-id>/odoo.conf` and `odoo-shell.conf` files can contain plaintext database/master/SMTP passwords. The plugin requests owner-only POSIX permissions where supported, but this is not encryption and filesystem/platform behavior varies. Deleting a saved project/runner does not delete generated files or automatically erase saved Password Safe entries. Clear saved passwords in the runner UI before removing definitions if needed; remove retained files separately. Backups or version-control tools may independently retain files.

Database browser/monitoring requests pass credentials to a local Python child process through stdin, not command-line arguments. Requests use the selected environment's psycopg2 to connect to the host you configure. That host may be remote. Database browsing/editing displays data you are authorized to access locally; direct SQL edits are sent only on explicit Save. HTTP runners/shells may use the network according to your Odoo configuration.

## External services you choose to use

- Pressing PyPI Find sends the exact package name to `https://pypi.org/pypi/<name>/json`; the service also receives normal network metadata such as your IP address. The plugin does not transmit project files, records or saved database credentials to PyPI.
- Installing dependencies runs pip after your action. pip uses your requirements and environment/configured package indexes; those may include third-party or private services with their own policies. Package installers can execute code. No dependency is installed automatically merely by opening the popup.
- Starting Odoo or other selected Python code executes that code with your account permissions. Its own behavior and data handling are outside this plugin's control.
- Contacting support sends only the information you choose to email, using your email provider. The plugin does not send support email automatically. The publisher may use that correspondence to respond; request its deletion via the contact address.

## Controls and limits

Stop runners/installations using their controls. Shell Back terminates that session; popup close ends popup shell sessions. Closing the IDE project stops its managed processes. Hiding Performance stops sampling. The popup may continue capturing a running HTTP process's bounded output while open/closed according to the project service lifetime. OS/IDE services and your configured database have their own logging policies.

Do not store `.odoo-runner/` in source control. Password redaction is best-effort, not a substitute for reviewing logs and reports. This document describes the implementation; it does not claim GDPR or other legal certification. Marketplace account/download handling is performed by JetBrains under its own policies.
