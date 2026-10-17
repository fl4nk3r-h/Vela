package dev.vela.cli;

import dev.vela.tooling.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ProjectInitializer {
    public void initialize(Path target, String packageName, Path repository) throws IOException {
        String name = target.toAbsolutePath().getFileName().toString();
        new ProjectConfig(packageName, packageName + ".MainScreen", name, 26, 35, 35);
        if (Files.exists(target)) try (var files = Files.list(target)) {
            if (files.findAny().isPresent()) throw new IOException("Directory is not empty: " + target);
        }
        Path absolute = target.toAbsolutePath(); Files.createDirectories(absolute.getParent());
        Path staging = Files.createTempDirectory(absolute.getParent(), ".vela-init-");
        try {
            write(staging, "vela.yaml", "package: " + packageName + "\nentry: " + packageName + ".MainScreen\nname: '" + name.replace("'", "''") + "'\nminSdk: 26\ncompileSdk: 35\ntargetSdk: 35\n");
            write(staging, "settings.gradle", """
                    pluginManagement {
                        repositories { maven { url = uri('.vela/repository') }; google(); mavenCentral(); gradlePluginPortal() }
                        plugins {
                            id 'com.android.application' version '8.13.2'
                            id 'dev.vela.android' version '0.1.0'
                        }
                    }
                    dependencyResolutionManagement {
                        repositories { maven { url = uri('.vela/repository') }; google(); mavenCentral() }
                    }
                    rootProject.name = 'vela-app'
                    """);
            write(staging, "build.gradle", "plugins { id 'com.android.application'; id 'dev.vela.android' }\n");
            write(staging, "gradle.properties", "org.gradle.configuration-cache=true\norg.gradle.caching=true\norg.gradle.jvmargs=-Xmx1536m\nandroid.useAndroidX=true\n");
            write(staging, ".gitignore", ".gradle/\nbuild/\n.vela/\nlocal.properties\n*.iml\n.idea/\n");
            write(staging, "src/main/vela/values.yaml", """
                    strings:
                      title: 'Hello Vela'
                      count: 'Count: {0,number,integer}'
                      increment: 'Increment'
                    colors:
                      background: '#F4F6F2'
                    dimensions:
                      spacing: 16dp
                    styles:
                      page:
                        padding: '@dimensions.spacing'
                        gap: '@dimensions.spacing'
                        background: '@colors.background'
                    """);
            write(staging, "src/main/java/" + packageName.replace('.', '/') + "/MainScreen.java", """
                    package %s;

                    import static dev.vela.ui.UI.*;
                    import dev.vela.core.*;

                    public final class MainScreen extends VelaComponent {
                        private final MutableState<Integer> count = state("count", 0);

                        @Override public ViewNode render() {
                            var values = Values.of(resources());
                            return column(
                                text(values.strings().title()),
                                text(values.strings().count(count.get())),
                                button(values.strings().increment(), () -> count.update(n -> n + 1))
                            ).with(values.styles().page());
                        }
                    }
                    """.formatted(packageName));
            write(staging, "README.md", "# " + name + "\n\nRun `vela setup`, `vela doctor`, then `vela dev`.\n\nEdit `src/main/java` and `src/main/vela/values.yaml`. Keyed state survives compatible reloads.\n\nUse `vela setup --emulator` to install an emulator; start it with Android Studio or `emulator -avd vela-api-35`.\n\n`vela run` installs debug; `vela test` runs checks; `vela build` builds release. Release signing is application configuration.\n");
            for (String path : List.of("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties")) {
                Path file = staging.resolve(path); Files.createDirectories(file.getParent());
                try (InputStream in = ProjectInitializer.class.getResourceAsStream("/bootstrap/" + path)) {
                    if (in == null) throw new IOException("CLI distribution is missing the Gradle wrapper: " + path);
                    Files.copy(in, file);
                }
            }
            staging.resolve("gradlew").toFile().setExecutable(true);
            if (repository != null && Files.isDirectory(repository)) copyTree(repository, staging.resolve(".vela/repository"));
            if (Files.exists(absolute)) Files.delete(absolute);
            Files.move(staging, absolute);
        } finally { if (Files.exists(staging)) deleteTree(staging); }
    }
    static void copyTree(Path source, Path target) throws IOException {
        try (var files = Files.walk(source)) {
            for (Path file : files.sorted().toList()) {
                Path destination = target.resolve(source.relativize(file));
                if (Files.isDirectory(file)) Files.createDirectories(destination);
                else { Files.createDirectories(destination.getParent()); Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING); }
            }
        }
    }
    static void deleteTree(Path directory) throws IOException {
        try (var files = Files.walk(directory)) { for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file); }
    }
    private static void write(Path directory, String path, String text) throws IOException {
        ResourceCompiler.writeIfChanged(directory.resolve(path), text.getBytes(StandardCharsets.UTF_8));
    }
}
