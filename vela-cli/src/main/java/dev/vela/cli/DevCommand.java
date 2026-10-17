package dev.vela.cli;

import dev.vela.dev.ReloadProtocol;
import dev.vela.tooling.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class DevCommand {
    private final Path project;
    private final ToolchainInstaller.Tools tools;
    private final String serial;
    private ProjectConfig config;
    private AdbClient adb;
    private final String token;
    DevCommand(Path project, ToolchainInstaller.Tools tools, String serial) throws IOException {
        this.project = project; this.tools = tools; this.serial = serial;
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); token = HexFormat.of().formatHex(bytes);
        Path asset = project.resolve(".vela/dev-assets/vela/token"); Files.createDirectories(asset.getParent()); Files.writeString(asset, token);
        config = ProjectConfig.read(project);
    }
    void run(boolean once) throws Exception {
        restart("Initial development build", false);
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Map<WatchKey, Path> keys = new HashMap<>(); register(project, watcher, keys);
            Map<Path, String> previous = snapshot();
            System.out.println("Vela ready. Edit Java or YAML; Ctrl+C stops watching.");
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watcher.poll(1, TimeUnit.SECONDS);
                if (key == null) continue;
                Set<Path> changed = new HashSet<>(); collect(key, keys, watcher, changed);
                while ((key = watcher.poll(180, TimeUnit.MILLISECONDS)) != null) collect(key, keys, watcher, changed);
                Map<Path, String> current = snapshot();
                changed.clear();
                Set<Path> paths = new HashSet<>(previous.keySet()); paths.addAll(current.keySet());
                for (Path path : paths) if (!Objects.equals(previous.get(path), current.get(path))) changed.add(path);
                previous = current;
                if (changed.isEmpty()) continue;
                long start = System.nanoTime();
                try { update(changed); }
                catch (Exception error) { System.err.println("Vela: " + error.getMessage() + "\nThe last working screen remains active. Fix the error and save again."); }
                System.out.printf(Locale.ROOT, "Edit processed in %.3f s%n", (System.nanoTime() - start) / 1e9);
                if (once) return;
            }
        }
    }
    private void update(Set<Path> changed) throws Exception {
        if (changed.stream().anyMatch(this::restartRequired)) { restart("Build configuration or packaged platform resource changed", true); return; }
        if (changed.stream().anyMatch(p -> p.toString().endsWith(".yaml")) && usesNativeResources()) {
            restart("YAML values are also consumed through native Android resource IDs", true); return;
        }
        boolean javaChanged = changed.stream().anyMatch(p -> p.toString().endsWith(".java"));
        if (changed.stream().anyMatch(p -> p.toString().endsWith(".yaml"))) {
            Path generated = project.resolve("build/generated/vela/resources/java/" + config.packageName().replace('.', '/') + "/Values.java");
            String before = Files.exists(generated) ? Files.readString(generated) : "";
            new ResourceCompiler().generate(project.resolve("src/main/vela"), config.packageName(), project.resolve("build/generated/vela/resources"));
            javaChanged |= !before.equals(Files.readString(generated));
        }
        byte[] resources = Files.readAllBytes(project.resolve("build/generated/vela/resources/assets/vela/values.bin"));
        ReloadProtocol.Request request;
        if (javaChanged) {
            Path marker = project.resolve("build/vela/reload/restart-required"); Files.deleteIfExists(marker);
            int status = gradle("velaReload");
            if (status != 0) {
                if (Files.exists(marker)) { restart("Reload payload requires a full Android build", true); return; }
                throw new IOException("Compilation failed");
            }
            Path dex = project.resolve("build/vela/reload/classes.dex");
            if (Files.size(dex) > ReloadProtocol.MAX_DEX) { restart("Reload payload exceeds the development size limit", true); return; }
            request = new ReloadProtocol.Request(ReloadProtocol.Operation.CODE, config.entry(), Files.readAllBytes(dex), resources,
                    Files.readAllLines(project.resolve("build/vela/reload/classes.list")));
        } else request = new ReloadProtocol.Request(ReloadProtocol.Operation.RESOURCES, "", new byte[0], resources);
        ReloadProtocol.Response response = adb.request(request);
        if (!response.success()) throw new IOException(response.message());
        System.out.println(response.message());
    }
    private boolean restartRequired(Path path) {
        String name = path.toString().replace('\\', '/');
        return !(name.startsWith("src/main/java/" + config.packageName().replace('.', '/') + "/") && name.endsWith(".java"))
                && !(name.startsWith("src/main/vela/") && name.endsWith(".yaml"));
    }
    private void restart(String reason, boolean checkpoint) throws Exception {
        System.out.println("Vela restart: " + reason);
        ProjectConfig next = ProjectConfig.read(project);
        if (gradle("assembleDebug") != 0) throw new IOException("Build failed");
        if (checkpoint && adb != null && config.packageName().equals(next.packageName())) try {
            var response = adb.request(new ReloadProtocol.Request(ReloadProtocol.Operation.CHECKPOINT, "", new byte[0], new byte[0]));
            if (!response.success()) System.err.println("State checkpoint unavailable: " + response.message());
        } catch (Exception error) { System.err.println("Device unavailable for checkpoint; reconnect and save again: " + error.getMessage()); }
        config = next;
        adb = new AdbClient(tools, project, AdbClient.device(tools, project, serial), config.packageName(), token);
        adb.installAndLaunch(); adb.awaitReady();
    }
    private int gradle(String task) throws Exception { return Vela.gradle(project, tools, task); }
    private static boolean ignored(Path path) {
        boolean source = false;
        for (Path part : path) {
            String name = part.toString();
            if (name.equals("src")) source = true;
            if (Set.of(".git", ".gradle", ".vela", ".idea").contains(name)
                    || (!source && Set.of("build", "bin").contains(name))) return true;
        }
        return false;
    }
    private void register(Path directory, WatchService watcher, Map<WatchKey, Path> keys) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path path, java.nio.file.attribute.BasicFileAttributes attributes) throws IOException {
                if (ignored(project.relativize(path))) return FileVisitResult.SKIP_SUBTREE;
                WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                keys.put(key, path);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path path, IOException error) throws IOException {
                if (error instanceof NoSuchFileException) return FileVisitResult.CONTINUE;
                throw error;
            }
        });
    }
    private void collect(WatchKey key, Map<WatchKey, Path> keys, WatchService watcher, Set<Path> changed) throws IOException {
        Path directory = keys.get(key);
        if (directory != null) for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
            Path path = directory.resolve((Path) event.context());
            if (ignored(project.relativize(path))) continue;
            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) {
                try { register(path, watcher, keys); } catch (NoSuchFileException ignored) { }
            }
            changed.add(project.relativize(path));
        }
        if (!key.reset()) keys.remove(key);
    }
    private Map<Path, String> snapshot() throws IOException {
        Map<Path, String> result = new HashMap<>();
        Files.walkFileTree(project, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path path, java.nio.file.attribute.BasicFileAttributes attributes) {
                return ignored(project.relativize(path)) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attributes) throws IOException {
                String name = file.getFileName().toString();
                if (name.endsWith("~") || name.endsWith(".swp") || name.endsWith(".swo") || name.endsWith(".tmp")) return FileVisitResult.CONTINUE;
                if (attributes.isRegularFile() && (name.endsWith(".java") || name.endsWith(".yaml") || name.endsWith(".xml") || name.endsWith(".gradle") || name.endsWith(".gradle.kts") || name.endsWith(".toml") || name.endsWith(".properties") || name.endsWith(".jar") || name.endsWith(".so") || name.startsWith("gradlew") || project.relativize(file).startsWith("src")))
                    try { result.put(project.relativize(file), VerifiedDownload.digest(file)); } catch (NoSuchFileException ignored) { }
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path path, IOException error) throws IOException {
                if (error instanceof NoSuchFileException) return FileVisitResult.CONTINUE;
                throw error;
            }
        });
        return result;
    }
    private boolean usesNativeResources() throws IOException {
        Path directory = project.resolve("src/main/java");
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (java.util.regex.Pattern.compile("\\bR\\s*\\.|\\bgetIdentifier\\s*\\(").matcher(Files.readString(file)).find()) return true;
            }
        }
        return false;
    }
}
