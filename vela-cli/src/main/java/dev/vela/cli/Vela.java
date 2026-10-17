package dev.vela.cli;

import dev.vela.tooling.ProjectConfig;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Vela {
    private Vela() { }
    public static void main(String[] args) {
        try { System.exit(execute(Path.of("").toAbsolutePath(), args)); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); System.exit(130); }
        catch (Exception error) { System.err.println("Vela: " + error.getMessage()); System.exit(1); }
    }
    public static int execute(Path directory, String... args) throws Exception {
        if (args.length == 0 || Set.of("help", "--help", "-h").contains(args[0])) {
            System.out.println("""
                    Vela 0.1.0 - Java-first Android development
                      vela init <directory> --package <com.example.app>
                      vela setup [--emulator] [--accept-licenses]
                      vela doctor
                      vela dev [--device <serial>] [--once]
                      vela run [--device <serial>]
                      vela test [--connected]
                      vela build
                    setup manages JDK 25 and Android SDK; dev retains compatible keyed state.
                    --once watches and processes one edit before exiting.
                    """); return 0;
        }
        Path cache = Path.of(System.getenv().getOrDefault("VELA_CACHE", Path.of(System.getProperty("user.home"), ".vela").toString()));
        ToolchainInstaller installer = new ToolchainInstaller(cache);
        List<String> options = List.of(args);
        if (args[0].equals("init")) {
            if (args.length < 2 || args[1].startsWith("-")) throw new IllegalArgumentException("Use vela init <directory> --package <com.example.app>");
            String pkg = option(options, "--package"); if (pkg == null) throw new IllegalArgumentException("--package is required");
            new ProjectInitializer().initialize(directory.resolve(args[1]), pkg, repository());
            System.out.println("Created " + args[1] + ". Run vela setup in that directory, then vela dev."); return 0;
        }
        ProjectConfig config = ProjectConfig.read(directory);
        if (args[0].equals("setup")) {
            var tools = installer.setup(directory, repository(), options.contains("--emulator"), options.contains("--accept-licenses"));
            System.out.println("JDK: " + tools.javaHome() + "\nAndroid SDK: " + tools.sdk() + "\nSetup complete. Run vela doctor."); return 0;
        }
        ToolchainInstaller.Tools tools = installer.locate(directory);
        switch (args[0]) {
            case "doctor": return doctor(directory, tools, config);
            case "dev": new DevCommand(directory, tools, option(options, "--device")).run(options.contains("--once")); return 0;
            case "run": {
                int status = gradle(directory, tools, "assembleDebug"); if (status != 0) return status;
                String serial = AdbClient.device(tools, directory, option(options, "--device"));
                new AdbClient(tools, directory, serial, config.packageName(), "").installAndLaunch(); return 0;
            }
            case "test": return options.contains("--connected") ? gradle(directory, tools, "test", "lintDebug", "connectedDebugAndroidTest") : gradle(directory, tools, "test", "lintDebug");
            case "build": return gradle(directory, tools, "assembleRelease");
            default: throw new IllegalArgumentException("Unknown command: " + args[0] + ". Use vela --help.");
        }
    }
    static int gradle(Path project, ToolchainInstaller.Tools tools, String... tasks) throws Exception {
        Path wrapper = project.resolve(ToolchainInstaller.windows() ? "gradlew.bat" : "gradlew");
        List<String> command = new ArrayList<>(Programs.command(wrapper)); command.addAll(List.of(tasks)); command.add("--console=plain");
        return Programs.run(command, project, tools.environment());
    }
    private static int doctor(Path project, ToolchainInstaller.Tools tools, ProjectConfig config) throws Exception {
        int missing = 0;
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("JDK 25 compiler", Files.isRegularFile(tools.javaHome().resolve("bin/javac" + (ToolchainInstaller.windows() ? ".exe" : ""))));
        checks.put("Android platform " + config.compileSdk(), Files.isRegularFile(tools.sdk().resolve("platforms/android-" + config.compileSdk() + "/android.jar")));
        checks.put("Android build tools 35.0.0", Files.isDirectory(tools.sdk().resolve("build-tools/35.0.0")));
        checks.put("ADB", Files.isRegularFile(tools.adb()));
        checks.put("Gradle wrapper", Files.isRegularFile(project.resolve("gradle/wrapper/gradle-wrapper.jar")));
        checks.put("Vela framework artifacts", Files.isDirectory(project.resolve(".vela/repository/dev/vela")));
        for (var check : checks.entrySet()) { System.out.println((check.getValue() ? "OK      " : "MISSING ") + check.getKey()); if (!check.getValue()) missing++; }
        if (Files.isRegularFile(tools.adb())) {
            try { System.out.println(Programs.capture(Programs.command(tools.adb(), "devices"), project, tools.environment())); }
            catch (IOException error) { System.out.println("ADB check failed: " + error.getMessage()); missing++; }
        }
        if (missing > 0) System.out.println("Run vela setup to install missing dependencies.");
        else System.out.println("Ready. Android Studio is optional; use a connected device or start an emulator.");
        return missing == 0 ? 0 : 1;
    }
    private static String option(List<String> args, String name) {
        int index = args.indexOf(name); if (index < 0) return null;
        if (index + 1 == args.size() || args.get(index + 1).startsWith("--")) throw new IllegalArgumentException("Missing value for " + name);
        return args.get(index + 1);
    }
    static Path repository() {
        String explicit = System.getProperty("vela.repository", System.getenv("VELA_REPOSITORY"));
        if (explicit != null) return Path.of(explicit);
        try {
            Path code = Path.of(Vela.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path dist = code.getParent().getParent(); Path repo = dist.resolve("repository");
            if (Files.isDirectory(repo)) return repo;
        } catch (Exception ignored) { }
        Path local = Path.of("build/repository").toAbsolutePath(); return Files.isDirectory(local) ? local : null;
    }
}
