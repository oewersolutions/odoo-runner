#!/usr/bin/env python3
"""Build against a locally installed PyCharm, without downloading SDKs or Gradle."""
import argparse
import os
import json
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("--ide", default=os.environ.get("PYCHARM_HOME", "/Applications/PyCharm.app/Contents"))
parser.add_argument("--jdk", default=os.environ.get("JAVA_HOME"))
args = parser.parse_args()
root = Path(__file__).resolve().parent
version = ET.parse(root / "src/main/resources/META-INF/plugin.xml").getroot().findtext("version")
if not version or not all(c.isalnum() or c in ".-_" for c in version):
    parser.error("plugin.xml must define a safe release version.")
ide = Path(args.ide).expanduser()
if (ide / "Contents").is_dir():
    ide = ide / "Contents"
sdk_build = ""
for candidate in (ide / "build.txt", ide / "Resources/build.txt"):
    if candidate.exists():
        sdk_build = candidate.read_text().strip().split("-")[-1]
        break
if not sdk_build:
    for candidate in (ide / "product-info.json", ide / "Resources/product-info.json"):
        if candidate.exists():
            sdk_build = json.loads(candidate.read_text()).get("buildNumber", "").split("-")[-1]
            break
if not sdk_build.startswith("232."):
    parser.error("This source targets PyCharm 2023.2. Pass --ide pointing to a build 232 SDK; do not compile this compatibility package against a newer SDK.")
jdk = Path(args.jdk) if args.jdk else next((candidate for candidate in
    [ide / "jbr/Contents/Home", ide / "jbr"] if (candidate / "bin/javac").exists()), Path("/missing-jdk"))
javac = jdk / "bin" / ("javac.exe" if os.name == "nt" else "javac")
if not javac.exists():
    parser.error("Pass --jdk with a JDK 17+ directory, or set JAVA_HOME.")
jars = list((ide / "lib").glob("*.jar")) + list((ide / "lib/modules").glob("*.jar"))
jars += list((ide / "plugins/terminal/lib").glob("*.jar"))
jars += list((ide / "plugins/vcs-git/lib").glob("*.jar"))
jars += list((ide / "plugins/git4idea/lib").glob("*.jar"))
if not jars:
    parser.error("No PyCharm libraries found; pass --ide with the IDE installation directory.")
classes = root / "build/classes"
if classes.exists():
    shutil.rmtree(classes)
classes.mkdir(parents=True)
sources = sorted((root / "src/main/java").rglob("*.java"))
compilation = subprocess.run([str(javac), "--release", "17", "-encoding", "UTF-8", "-classpath",
    os.pathsep.join(map(str, jars)), "-d", str(classes), *map(str, sources)])
if compilation.returncode:
    sys.exit(compilation.returncode)
resources = root / "src/main/resources"
jar = root / "build/odoo-runner.jar"
with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as archive:
    for directory in (classes, resources):
        for path in sorted(directory.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(directory))
    archive.write(root / "LICENSE", "META-INF/LICENSE")
dist = root / "dist"
dist.mkdir(exist_ok=True)
output = dist / f"odoo-runner-{version}.zip"
with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
    archive.write(jar, "odoo-runner/lib/odoo-runner.jar")
    for name in ("README.md", "LICENSE", "CHANGELOG.md", "PRIVACY.md", "SECURITY.md", "THIRD_PARTY_NOTICES.md", "CONTRIBUTING.md"):
        path = root / name
        if path.is_file():
            archive.write(path, "odoo-runner/" + name)
    for path in sorted((root / "docs").rglob("*")):
        if path.is_file() and path.suffix in (".md", ".txt"):
            archive.write(path, "odoo-runner/" + str(path.relative_to(root)))
print(f"Installable plugin: {output}")
