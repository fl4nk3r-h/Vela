#!/usr/bin/env python3
"""Check a platform CLI archive without using a system Java installation."""
import argparse
import os
from pathlib import Path
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("--archive", required=True, type=Path)
parser.add_argument("--work-dir", required=True, type=Path)
args = parser.parse_args()
work = args.work_dir.resolve()
work.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(args.archive) as archive:
    if os.name != "nt":
        for path in ("vela/bin/vela", "vela/runtime/bin/java"):
            assert archive.getinfo(path).external_attr >> 16 & 0o111, f"Missing executable permission: {path}"
    archive.extractall(work)
    if os.name != "nt":
        for entry in archive.infolist():
            mode = entry.external_attr >> 16 & 0o777
            if mode:
                (work / entry.filename).chmod(mode)
launcher = work / "vela/bin" / ("vela.bat" if os.name == "nt" else "vela")
command = [str(launcher)] if os.name != "nt" else [os.environ.get("COMSPEC", "cmd.exe"), "/c", str(launcher)]
environment = os.environ.copy()
environment["JAVA_HOME"] = str(work / "no-installed-jdk")
subprocess.run(command + ["--help"], env=environment, check=True)
subprocess.run(command + ["init", str(work / "application with spaces"), "--package", "example.archive"], env=environment, check=True)
assert (work / "application with spaces/.vela/repository/dev/vela/vela-android").is_dir()
print("Bundled runtime, executable permissions, initializer, and framework repository verified")
