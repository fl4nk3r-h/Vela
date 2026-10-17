package dev.vela.cli;

import dev.vela.dev.ReloadProtocol;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

final class AdbClient {
    private final ToolchainInstaller.Tools tools;
    private final Path project;
    private final String serial;
    private final String packageName;
    private final String token;
    AdbClient(ToolchainInstaller.Tools tools, Path project, String serial, String packageName, String token) {
        this.tools = tools; this.project = project; this.serial = serial; this.packageName = packageName; this.token = token;
    }
    static String device(ToolchainInstaller.Tools tools, Path project, String requested) throws Exception {
        if (requested != null) return requested;
        String devices = Programs.capture(Programs.command(tools.adb(), "devices"), project, tools.environment());
        List<String> available = devices.lines().map(String::strip).filter(s -> s.matches("\\S+\\s+device"))
                .map(s -> s.split("\\s+")[0]).toList();
        if (available.size() != 1) throw new IOException(available.isEmpty() ? "No authorized Android device. Start an emulator or enable USB debugging."
                : "Multiple Android devices; specify --device " + available.get(0));
        return available.get(0);
    }
    String command(String... arguments) throws Exception {
        List<String> command = new ArrayList<>(Programs.command(tools.adb(), "-s", serial)); command.addAll(List.of(arguments));
        return Programs.capture(command, project, tools.environment());
    }
    void installAndLaunch() throws Exception {
        Path apk = project.resolve("build/outputs/apk/debug/" + project.getFileName() + "-debug.apk");
        Path output = project.resolve("build/outputs/apk/debug");
        if (!Files.isRegularFile(apk) && Files.isDirectory(output)) try (var files = Files.list(output)) {
            apk = files.filter(p -> p.toString().endsWith(".apk")).findFirst().orElseThrow(() -> new IOException("Debug APK is missing"));
        }
        command("install", "-r", apk.toString());
        command("shell", "am", "force-stop", packageName);
        command("shell", "am", "start", "-W", "-n", packageName + "/dev.vela.android.VelaActivity");
    }
    ReloadProtocol.Response request(ReloadProtocol.Request request) throws Exception {
        String portText = command("forward", "tcp:0", "localabstract:vela." + packageName);
        int port;
        try { port = Integer.parseInt(portText.strip()); } catch (NumberFormatException error) { throw new IOException("Cannot establish ADB development connection: " + portText); }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 5000); socket.setSoTimeout(20000);
            ReloadProtocol.writeRequest(new DataOutputStream(socket.getOutputStream()), token, request);
            try { return ReloadProtocol.readResponse(new DataInputStream(socket.getInputStream())); }
            catch (EOFException error) { throw new IOException("Development bridge disconnected. Check the device and adb logcat -s Vela, then save again.", error); }
        } finally { try { command("forward", "--remove", "tcp:" + port); } catch (Exception ignored) { } }
    }
    void awaitReady() throws Exception {
        Exception last = null;
        for (int i = 0; i < 30; i++) try {
            if (request(new ReloadProtocol.Request(ReloadProtocol.Operation.PING, "", new byte[0], new byte[0])).success()) return;
        } catch (Exception error) { last = error; Thread.sleep(200); }
        throw new IOException("Development bridge did not start. Check adb logcat -s Vela and ensure a debug APK is running.", last);
    }
}
