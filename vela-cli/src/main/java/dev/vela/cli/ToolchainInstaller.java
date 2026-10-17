package dev.vela.cli;

import dev.vela.tooling.ProjectConfig;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.nio.channels.FileChannel;

public final class ToolchainInstaller {
    private final Path cache;
    private final Properties manifest;
    private final Map<String, String> environment;
    public record Tools(Path javaHome, Path sdk) {
        public Map<String, String> environment() {
            String path = javaHome.resolve("bin") + File.pathSeparator + sdk.resolve("platform-tools") + File.pathSeparator
                    + sdk.resolve("emulator") + File.pathSeparator + System.getenv().getOrDefault("PATH", "");
            return Map.of("JAVA_HOME", javaHome.toString(), "ANDROID_HOME", sdk.toString(), "ANDROID_SDK_ROOT", sdk.toString(), "PATH", path);
        }
        public Path adb() { return sdk.resolve("platform-tools/adb" + (windows() ? ".exe" : "")); }
    }
    public ToolchainInstaller(Path cache) throws IOException {
        this.cache = cache; environment = System.getenv(); manifest = new Properties();
        try (InputStream in = ToolchainInstaller.class.getResourceAsStream("/toolchains.properties")) {
            if (in == null) throw new IOException("Toolchain manifest is missing"); manifest.load(in);
        }
    }
    public ToolchainInstaller(Path cache, Properties manifest, Map<String, String> environment) {
        this.cache = cache; this.manifest = manifest; this.environment = environment;
    }
    public Tools setup(Path project, Path repository, boolean emulator, boolean acceptLicenses) throws Exception {
        ProjectConfig config = ProjectConfig.read(project);
        Path javaHome = existingJava();
        if (javaHome == null) {
            System.out.println("Installing managed JDK 25...");
            Path installation = install("jdk", cache.resolve("jdk/25"));
            try (var files = Files.walk(installation, 5)) {
                javaHome = files.filter(p -> p.getFileName().toString().equals("javac" + (windows() ? ".exe" : "")))
                        .findFirst().orElseThrow(() -> new IOException("JDK archive contains no compiler")).getParent().getParent();
            }
        }
        Path sdk = existingSdk();
        if (sdk == null) sdk = cache.resolve("android-sdk");
        Tools tools = new Tools(javaHome.toAbsolutePath(), sdk.toAbsolutePath());
        boolean packagesMissing = !Files.isRegularFile(sdk.resolve("platforms/android-" + config.compileSdk() + "/android.jar"))
                || !Files.isRegularFile(tools.adb()) || !Files.isRegularFile(sdk.resolve("build-tools/35.0.0/d8" + (windows() ? ".bat" : "")));
        if (packagesMissing || emulator) {
            Path manager = sdk.resolve("cmdline-tools/latest/bin/sdkmanager" + (windows() ? ".bat" : ""));
            if (!Files.exists(manager)) {
                Path unpacked = install("sdk", cache.resolve("android-command-tools/" + manifest.getProperty("sdk.tools.version", "22.0")));
                ProjectInitializer.copyTree(unpacked.resolve("cmdline-tools"), sdk.resolve("cmdline-tools/latest"));
            }
            List<String> licenses = sdkCommand(javaHome, manager, "sdkmanager", "--sdk_root=" + sdk, "--licenses");
            ProcessBuilder builder = new ProcessBuilder(licenses).directory(project.toFile()); builder.environment().putAll(tools.environment());
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT);
            if (!acceptLicenses) {
                System.out.println("Review and accept the Android SDK licenses below, or rerun with --accept-licenses after reviewing them.");
                builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            }
            Process process = builder.start();
            if (acceptLicenses) try (OutputStream out = process.getOutputStream()) { out.write("y\n".repeat(100).getBytes(java.nio.charset.StandardCharsets.US_ASCII)); }
            if (process.waitFor() != 0) throw new IOException("Android SDK licenses were not accepted");
            List<String> packages = new ArrayList<>(sdkCommand(javaHome, manager, "sdkmanager", "--sdk_root=" + sdk, "platform-tools", "platforms;android-" + config.compileSdk(), "build-tools;35.0.0"));
            String image = "system-images;android-35;google_apis;" + (architecture().equals("aarch64") ? "arm64-v8a" : "x86_64");
            if (emulator) packages.addAll(List.of("emulator", image));
            if (Programs.run(packages, project, tools.environment()) != 0) throw new IOException("Android SDK package installation failed");
            if (!Files.isRegularFile(tools.adb()) || !Files.isRegularFile(sdk.resolve("platforms/android-" + config.compileSdk() + "/android.jar"))
                    || !Files.isRegularFile(sdk.resolve("build-tools/35.0.0/d8" + (windows() ? ".bat" : ""))))
                throw new IOException("Required SDK packages are missing; review the package licenses and rerun vela setup");
            if (emulator) {
                Path avdManager = manager.resolveSibling("avdmanager" + (windows() ? ".bat" : ""));
                String installed = Programs.capture(sdkCommand(javaHome, avdManager, "avdmanager", "list", "avd"), project, tools.environment());
                if (!installed.contains("Name: vela-api-35")) {
                    Path avdPath = cache.resolve("avds/vela-api-35.avd").toAbsolutePath(); Files.createDirectories(avdPath.getParent());
                    ProcessBuilder avd = new ProcessBuilder(sdkCommand(javaHome, avdManager, "avdmanager", "create", "avd", "--name", "vela-api-35", "--package", image, "--path", avdPath.toString()));
                    avd.environment().putAll(tools.environment()); avd.redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT);
                    Process create = avd.start(); try (OutputStream out = create.getOutputStream()) { out.write("no\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII)); }
                    if (create.waitFor() != 0) throw new IOException("Emulator creation failed; verify the system image and available disk space");
                }
            }
        }
        Files.createDirectories(project.resolve(".vela"));
        Properties local = new Properties(); local.setProperty("java.home", tools.javaHome().toString()); local.setProperty("sdk.dir", tools.sdk().toString());
        try (OutputStream out = Files.newOutputStream(project.resolve(".vela/tools.properties"))) { local.store(out, "Vela local managed toolchain"); }
        Properties android = new Properties(); android.setProperty("sdk.dir", tools.sdk().toString());
        try (OutputStream out = Files.newOutputStream(project.resolve("local.properties"))) { android.store(out, "Android SDK"); }
        if (repository != null && Files.isDirectory(repository)) ProjectInitializer.copyTree(repository, project.resolve(".vela/repository"));
        return tools;
    }
    public Tools locate(Path project) throws IOException {
        Properties local = new Properties(); Path file = project.resolve(".vela/tools.properties");
        if (Files.exists(file)) try (InputStream in = Files.newInputStream(file)) { local.load(in); }
        Path javaHome = local.containsKey("java.home") ? Path.of(local.getProperty("java.home")) : existingJava();
        Path sdk = local.containsKey("sdk.dir") ? Path.of(local.getProperty("sdk.dir")) : existingSdk();
        if (javaHome == null || sdk == null) throw new IOException("Missing JDK 25 or Android SDK. Run vela setup.");
        return new Tools(javaHome, sdk);
    }
    public Path install(String kind, Path target) throws Exception {
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (FileChannel channel = FileChannel.open(target.resolveSibling(target.getFileName() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            return installLocked(kind, target);
        }
    }
    private Path installLocked(String kind, Path target) throws Exception {
        if (Files.isRegularFile(target.resolve(".complete"))) return target;
        String prefix = kind + "." + operatingSystem() + "." + architecture();
        String url = manifest.getProperty(prefix + ".url");
        if (url == null) throw new IOException("No managed " + kind + " archive for " + operatingSystem() + "/" + architecture() + "; use an existing installation");
        String checksum = manifest.getProperty(prefix + ".sha256", "");
        String extension = URI.create(url).getPath().endsWith(".zip") ? ".zip" : ".tar.gz";
        Path archive = new VerifiedDownload().download(URI.create(url), checksum, cache.resolve("downloads/" + checksum + extension));
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path staging = Files.createTempDirectory(target.toAbsolutePath().getParent(), ".install-");
        try {
            Archives.extract(archive, staging); Files.writeString(staging.resolve(".complete"), checksum);
            if (Files.exists(target)) ProjectInitializer.deleteTree(target);
            Files.move(staging, target); return target;
        } finally { if (Files.exists(staging)) ProjectInitializer.deleteTree(staging); }
    }
    private static List<String> sdkCommand(Path javaHome, Path manager, String tool, String... arguments) {
        Path home = manager.getParent().getParent();
        Path java = javaHome.resolve("bin/java" + (windows() ? ".exe" : ""));
        String entry = tool.equals("sdkmanager") ? "com.android.sdklib.tool.sdkmanager.SdkManagerCli" : "com.android.sdklib.tool.AvdManagerCli";
        String toolsProperty = tool.equals("sdkmanager") ? "com.android.sdklib.toolsdir" : "com.android.sdkmanager.toolsdir";
        return Programs.command(java, combine(new String[]{"-Xmx512m", "-D" + toolsProperty + "=" + home, "-classpath", home.resolve("lib/" + tool + "-classpath.jar").toString(), entry}, arguments));
    }
    private static String[] combine(String[] prefix, String[] arguments) {
        String[] result = Arrays.copyOf(prefix, prefix.length + arguments.length);
        System.arraycopy(arguments, 0, result, prefix.length, arguments.length); return result;
    }
    private Path existingJava() {
        List<Path> candidates = new ArrayList<>();
        if (environment.containsKey("JAVA_HOME")) candidates.add(Path.of(environment.get("JAVA_HOME")));
        if (Files.isDirectory(cache.resolve("jdk/25"))) try (var files = Files.walk(cache.resolve("jdk/25"), 5)) {
            files.filter(p -> p.getFileName().toString().equals("javac" + (windows() ? ".exe" : "")))
                    .map(p -> p.getParent().getParent()).forEach(candidates::add);
        } catch (IOException ignored) { }
        candidates.add(Path.of(System.getProperty("java.home")));
        for (Path candidate : candidates) try {
            if (!Files.isRegularFile(candidate.resolve("bin/javac" + (windows() ? ".exe" : "")))) continue;
            Properties release = new Properties(); try (Reader in = Files.newBufferedReader(candidate.resolve("release"))) { release.load(in); }
            if (release.getProperty("JAVA_VERSION", "").replace("\"", "").matches("25(?:\\..*)?")) return candidate.toAbsolutePath();
        } catch (IOException ignored) { }
        return null;
    }
    private Path existingSdk() {
        for (String name : List.of("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
            String path = environment.get(name); if (path != null && !path.isBlank() && Files.isDirectory(Path.of(path))) return Path.of(path).toAbsolutePath();
        }
        return Files.isDirectory(cache.resolve("android-sdk")) ? cache.resolve("android-sdk") : null;
    }
    static boolean windows() { return operatingSystem().equals("windows"); }
    static String operatingSystem() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "mac";
        if (os.contains("linux")) return "linux";
        throw new IllegalStateException("Unsupported OS: " + os);
    }
    static String architecture() {
        String arch = System.getProperty("os.arch");
        if (Set.of("amd64", "x86_64").contains(arch)) return "x64";
        if (Set.of("aarch64", "arm64").contains(arch)) return "aarch64";
        throw new IllegalStateException("Unsupported architecture: " + arch);
    }
}
