#!/usr/bin/env python3
"""Local distribution checks; complements (never replaces) JetBrains Plugin Verifier."""
import argparse
import io
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('archive', type=Path)
parser.add_argument('--marketplace', action='store_true', help='Fail for pending publication URLs too')
parser.add_argument('--source-url', help='Public source repository URL (reachability checked separately by publisher)')
args = parser.parse_args()
errors, pending = [], []

def check(ok, text):
    if not ok:
        errors.append(text)

with zipfile.ZipFile(args.archive) as distribution:
    names = distribution.namelist()
    check(all(name.startswith('odoo-runner/') for name in names), 'ZIP must have exactly the plugin root directory.')
    jars = [name for name in names if name.endswith('.jar')]
    check(jars == ['odoo-runner/lib/odoo-runner.jar'], 'Do not bundle SDK/third-party libraries in this package.')
    for name in ['README.md', 'LICENSE', 'CHANGELOG.md', 'PRIVACY.md', 'SECURITY.md', 'THIRD_PARTY_NOTICES.md']:
        check('odoo-runner/' + name in names, 'Missing distribution document: ' + name)
    with zipfile.ZipFile(io.BytesIO(distribution.read('odoo-runner/lib/odoo-runner.jar'))) as jar:
        metadata = ET.fromstring(jar.read('META-INF/plugin.xml'))
        version = metadata.findtext('version')
        check(metadata.findtext('id') == 'dev.odoorunner', 'Unexpected plugin ID; updates must retain the ID.')
        check(args.archive.name == f'odoo-runner-{version}.zip', 'Archive and descriptor versions differ.')
        check(len(metadata.findtext('name', '')) <= 30, 'Marketplace names are limited to 30 characters.')
        check(len(metadata.findtext('description', '').strip()) >= 40, 'A meaningful English description is required.')
        check(bool(metadata.findtext('change-notes', '').strip()), 'Missing release change notes.')
        vendor = metadata.find('vendor')
        check(vendor is not None and vendor.text == 'Oewer Solutions', 'Unexpected author/vendor name.')
        check(vendor is not None and vendor.get('email') == 'oewersolutions@gmail.com', 'Missing vendor contact email.')
        if vendor is None or not re.match(r'https://[^/]+', vendor.get('url', '')):
            pending.append('Vendor website URL remains unset; owner deferred it.')
        if not args.source_url or not re.match(r'https://[^/]+', args.source_url):
            pending.append('Public MIT source repository URL remains unset; owner deferred it.')
        dependencies = {node.text for node in metadata.findall('depends')}
        check({'com.intellij.modules.platform', 'com.intellij.modules.python', 'org.jetbrains.plugins.terminal'} <= dependencies, 'Missing required platform/Python/Terminal dependency.')
        bounds = metadata.find('idea-version')
        check(bounds is not None and bounds.get('since-build') == '232.10072.31' and bounds.get('until-build') == '232.*', 'Unexpected/unverified IDE compatibility bounds.')
        for name in ['META-INF/pluginIcon.svg', 'META-INF/pluginIcon_dark.svg']:
            raw = jar.read(name)
            icon = ET.fromstring(raw)
            check(icon.get('width') == '40' and icon.get('height') == '40', 'Plugin-manager icons must be 40x40 SVG.')
            check(len(raw) < 3000 and b'<script' not in raw and b'http' not in raw.replace(b'http://www.w3.org/2000/svg', b''), 'Icon must be compact and self-contained.')
        check('MIT License' in jar.read('META-INF/LICENSE').decode(), 'Missing packaged MIT license.')
        check('Copyright (c) 2026 Oewer Solutions' in distribution.read('odoo-runner/LICENSE').decode(), 'Missing copyright holder.')
        for name in jar.namelist():
            if name.endswith('.class'):
                data = jar.read(name)
                check(data[:4] == b'\xca\xfe\xba\xbe' and int.from_bytes(data[6:8], 'big') == 61, 'Unexpected non-Java17 class: ' + name)
        compile(jar.read('database/browser.py'), 'database/browser.py', 'exec')

for message in errors:
    print('ERROR:', message)
for message in pending:
    print('PENDING:', message)
print('Package/descriptor/license/icon/Java17/Python syntax checks:', 'PASS' if not errors else 'FAIL')
print('Live IDE functionality, binary compatibility, public link reachability and Marketplace approval are separate checks.')
sys.exit(1 if errors or (args.marketplace and pending) else 0)
