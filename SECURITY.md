# Security reporting and operating notes

Report security issues privately to **oewersolutions@gmail.com**. Include the affected plugin/IDE version, a sanitized description and reproduction steps. Do not send credentials or business records. No response-time commitment is implied.

Run only source code, interpreters, requirements and addon folders you trust. Odoo shell commands and pip packages execute with your user permissions. Use appropriate database roles. The record editor performs direct SQL, not Odoo ORM writes, and does not run Odoo's business/access rules or recompute/caching hooks.

Password fields use IDE Password Safe. Odoo needs generated configuration files which can contain plaintext secrets. Keep `.odoo-runner/` excluded from version control and backups shared with others. Generated configs/logs persist unless removed. Redaction is best-effort. Avoid displaying/copying private records into bug reports.

The current compatibility declaration is only PyCharm build232. Compatibility checks are not a security audit and do not establish safe operation on newer IDEs. See docs/MARKETPLACE_READINESS.md for known release limits and verification evidence.
