#!/usr/bin/env python3
"""Measure clean, unchanged, and full restart builds of a disposable generated app."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument("--project", required=True, type=Path)
parser.add_argument("--cli", required=True, type=Path)
parser.add_argument("--device", required=True)
parser.add_argument("--output", required=True, type=Path)
args = parser.parse_args()
project = args.project.resolve()
java_home = Path(os.environ["JAVA_HOME"])
java = java_home / "bin" / ("java.exe" if os.name == "nt" else "java")
wrapper = [str(java), "-Xmx64m", "-classpath", str(project / "gradle/wrapper/gradle-wrapper.jar"), "org.gradle.wrapper.GradleWrapperMain"]
log = args.output.resolve().with_suffix(".log")


def measure(command):
    start = time.monotonic()
    result = subprocess.run(command, cwd=project, capture_output=True, text=True, timeout=240)
    with log.open("a") as output:
        output.write(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    return round(time.monotonic() - start, 3), result.stdout


results = {}
results["clean_debug_build_seconds"], _ = measure(wrapper + ["clean", "assembleDebug", "--no-build-cache", "--console=plain"])
results["unchanged_debug_build_seconds"], output = measure(wrapper + ["assembleDebug", "--no-build-cache", "--console=plain"])
for task in ("compileDebugJavaWithJavac", "generateVelaResources"):
    assert re.search(rf":{task} UP-TO-DATE", output), f"Unchanged {task} executed"
source = next((project / "src/main/java").rglob("MainScreen.java"))
original = source.read_text()
changed = re.sub(r"Method v\d+", "Method full restart", original)
if original == changed:
    raise RuntimeError("Use a disposable project produced by verify_dev_workflow.py")
try:
    source.write_text(changed)
    results["java_edit_full_rebuild_restart_seconds"], _ = measure([str(args.cli.resolve()), "run", "--device", args.device])
finally:
    source.write_text(original)
args.output.write_text(json.dumps(results, indent=2) + "\n")
print(json.dumps(results, indent=2))
