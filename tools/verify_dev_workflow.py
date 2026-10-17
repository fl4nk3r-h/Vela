#!/usr/bin/env python3
"""Exercise a generated application through the real CLI, ADB bridge, and Android UI."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile


SCREEN = '''package dev.vela.fixture;
import static dev.vela.ui.UI.*;
import dev.vela.core.*;
public final class MainScreen extends VelaComponent {
    private final MutableState<Integer> count = state("count", 0);
    private final MutableState<String> name = state("name", "");
    private final Child child = new Child();
    private record Label(String text) { }
    @Override public ViewNode render() {
        var values = Values.of(resources());
        return column(
            text(values.strings().title()),
            text(new Label(java.util.List.of("Method v1").stream().toList().get(0)).text()).withKey("method"),
            text("Count: " + count.get()),
            button("Increment", () -> count.update(n -> n + 1)),
            field("Name", name).withKey("name"),
            child("child", child)
        ).with(values.styles().page());
    }
    public static final class Child extends VelaComponent {
        private final MutableState<Integer> count = state("count", 0);
        @Override public ViewNode render() {
            return column(text("Child: " + count.get()), button("Child increment", () -> count.update(n -> n + 1)));
        }
    }
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--cli", required=True, type=Path)
    parser.add_argument("--work-dir", required=True, type=Path)
    parser.add_argument("--device", default=os.environ.get("ANDROID_SERIAL", "emulator-5554"))
    args = parser.parse_args()
    cli = args.cli.resolve()
    work = args.work_dir.resolve()
    work.mkdir(parents=True, exist_ok=True)
    project = work / "project with spaces"
    sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
    adb = sdk / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb")
    environment = os.environ.copy()
    environment["VELA_CACHE"] = str(work / "tool-cache")

    def run(command, cwd=project):
        result = subprocess.run(list(map(str, command)), cwd=cwd, env=environment, capture_output=True, text=True, timeout=240)
        if result.returncode:
            raise RuntimeError(f"Command failed: {command}\n{result.stdout}\n{result.stderr}")
        return result.stdout

    def android(*command):
        return run([adb, "-s", args.device, *command])

    if project.exists():
        raise RuntimeError(f"Use a fresh work directory: {project}")
    run([cli, "init", project, "--package", "dev.vela.fixture"], work)
    run([cli, "setup"])
    run([cli, "doctor"])
    source = project / "src/main/java/dev/vela/fixture/MainScreen.java"
    source.write_text(SCREEN)
    log_path = work / "dev.log"
    log = log_path.open("w")
    process = subprocess.Popen([str(cli), "dev", "--device", args.device], cwd=project, env=environment, stdout=log, stderr=subprocess.STDOUT)
    results = {}

    def wait_log(text, offset=0, timeout=120):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            content = log_path.read_text(errors="replace")
            if text in content[offset:]:
                return content
            if process.poll() is not None:
                raise RuntimeError(f"vela dev exited\n{content}")
            time.sleep(0.1)
        raise RuntimeError(f"Timed out waiting for {text}\n{log_path.read_text(errors='replace')}")

    def nodes():
        android("shell", "uiautomator", "dump", "/data/local/tmp/vela-ui.xml")
        return ET.fromstring(android("exec-out", "cat", "/data/local/tmp/vela-ui.xml")).iter("node")

    def expect(*texts):
        found = {node.attrib.get("text", "") for node in nodes()}
        for text in texts:
            assert text in found, f"Missing {text!r}; found {found}"

    def tap(text):
        for node in nodes():
            if node.attrib.get("text", "").casefold() == text.casefold():
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
                android("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                return
        raise AssertionError(f"Cannot find button {text}")

    def edit(text, expected="Reloaded"):
        offset = len(log_path.read_text(errors="replace"))
        source.write_text(text)
        content = wait_log(expected, offset)
        content = wait_log("Edit processed in", offset)
        return float(re.findall(r"Edit processed in ([\d.]+) s", content[offset:])[-1])

    try:
        start = time.monotonic()
        wait_log("Vela ready", timeout=180)
        results["initial_dev_seconds"] = round(time.monotonic() - start, 3)
        pid = android("shell", "pidof", "dev.vela.fixture").strip()
        assert pid
        tap("Increment")
        tap("Child increment")
        expect("Count: 1", "Child: 1", "Method v1")
        # Text entry and native selection/focus have additional instrumented coverage.
        for node in nodes():
            if node.attrib.get("class") == "android.widget.EditText":
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
                android("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                android("shell", "input", "text", "Ada")
                android("shell", "input", "keyevent", "KEYCODE_BACK")
                break
        expect("Ada")
        classes = list((project / "build/intermediates/javac").rglob("MainScreen.class"))
        assert classes
        modified = {file: file.stat().st_mtime_ns for file in classes}
        apk = next((project / "build/outputs/apk/debug").glob("*.apk"))
        apk_mtime = apk.stat().st_mtime_ns
        values = project / "src/main/vela/values.yaml"
        offset = len(log_path.read_text(errors="replace"))
        values.write_text(values.read_text().replace("Hello Vela", "YAML v2"))
        content = wait_log("Resources refreshed", offset)
        content = wait_log("Edit processed in", offset)
        results["yaml_value_seconds"] = float(re.findall(r"Edit processed in ([\d.]+) s", content[offset:])[-1])
        assert all(file.stat().st_mtime_ns == stamp for file, stamp in modified.items()), "Value edit compiled Java"
        expect("YAML v2", "Count: 1", "Child: 1", "Ada")
        screen = SCREEN.replace("Method v1", "Method v2")
        results["method_edit_seconds"] = edit(screen)
        expect("Method v2", "Count: 1", "Child: 1", "Ada")
        screen = screen.replace('private final Child child', 'private final MutableState<Integer> added = state("added", 7);\n    private final Child child')
        screen = screen.replace('text("Count: " + count.get()),', 'text("Count: " + count.get()),\n            text("Added: " + added.get()).withKey("added"),')
        results["field_edit_seconds"] = edit(screen)
        expect("Added: 7", "Count: 1", "Child: 1", "Ada")
        edit(screen + "\ninvalid Java\n", "Compilation failed")
        expect("Added: 7", "Count: 1", "Child: 1")
        edit(screen)
        broken = screen.replace('new Label(java.util.List.of("Method v2").stream().toList().get(0)).text()', 'broken()').replace('public static final class Child', 'private String broken() { throw new IllegalStateException("Rejected candidate"); }\n    public static final class Child')
        edit(broken, "Rejected candidate")
        expect("Method v2", "Count: 1", "Child: 1", "Ada")
        edit(screen)
        for version in range(3, 6):
            screen = screen.replace(f"Method v{version-1}", f"Method v{version}")
            edit(screen)
            expect(f"Method v{version}", "Count: 1", "Child: 1", "Ada")
        assert android("shell", "pidof", "dev.vela.fixture").strip() == pid, "Supported edits restarted app"
        assert apk.stat().st_mtime_ns == apk_mtime, "Supported edits assembled an APK"
        android("shell", "settings", "put", "system", "accelerometer_rotation", "0")
        android("shell", "settings", "put", "system", "user_rotation", "1")
        time.sleep(1)
        android("shell", "settings", "put", "system", "user_rotation", "0")
        time.sleep(1)
        expect("Method v5", "YAML v2", "Added: 7", "Count: 1", "Child: 1", "Ada")
        screen = screen.replace('MutableState<Integer> count = state("count", 0);', 'MutableState<Long> count = state("count", 0L);', 1)
        edit(screen, "Reset incompatible state: count")
        expect("Count: 0", "Child: 1", "Ada")
        tap("Increment")
        config = project / "vela.yaml"
        offset = len(log_path.read_text(errors="replace"))
        config.write_text(re.sub(r"^name:.*$", "name: 'Restarted Vela'", config.read_text(), flags=re.M))
        wait_log("Vela restart: Build configuration", offset)
        wait_log("Edit processed in", offset)
        expect("Count: 1", "Child: 1", "Ada")
        assert android("shell", "pidof", "dev.vela.fixture").strip() != pid, "Platform edit did not restart app"
        # A fresh forward is established per request; dropping the device transport must recover.
        android("reconnect")
        android("wait-for-device")
        screen = screen.replace("Method v5", "Method v6")
        edit(screen)
        expect("Method v6", "Count: 1", "Child: 1", "Ada")
        run([cli, "build"])
        release = next((project / "build/outputs/apk/release").glob("*.apk"))
        with zipfile.ZipFile(release) as archive:
            for name in archive.namelist():
                if name.endswith(".dex"):
                    data = archive.read(name)
                    for forbidden in (b"Ldev/vela/devtools/DevelopmentSession;", b"Ldev/vela/dev/ReloadProtocol;", b"Ldalvik/system/InMemoryDexClassLoader;"):
                        assert forbidden not in data, f"Development implementation leaked into release: {forbidden}"
        results["release_excludes_devtools"] = True
        results["state_and_failure_scenarios"] = "passed"
        (work / "results.json").write_text(json.dumps(results, indent=2) + "\n")
        print(json.dumps(results, indent=2))
    finally:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
        log.close()
        android("shell", "am", "force-stop", "dev.vela.fixture")


if __name__ == "__main__":
    main()
