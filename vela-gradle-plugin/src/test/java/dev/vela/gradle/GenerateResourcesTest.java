package dev.vela.gradle;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.gradle.testkit.runner.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateResourcesTest {
    @TempDir Path directory;
    @Test void resourceTaskReusesConfigurationAndBuildCacheWithoutRewritingJavaForValues() throws Exception {
        Files.writeString(directory.resolve("settings.gradle"), "rootProject.name = 'resources-test'\n");
        Files.writeString(directory.resolve("build.gradle"), """
                plugins { id 'dev.vela.android' apply false }
                tasks.register('generate', dev.vela.gradle.GenerateResources) {
                    inputDirectory.set(layout.projectDirectory.dir('values'))
                    resourcePackage.set('example.app')
                    outputDirectory.set(layout.buildDirectory.dir('generated'))
                }
                """);
        Files.createDirectories(directory.resolve("values")); Path yaml = directory.resolve("values/values.yaml"); Files.writeString(yaml, "strings:\n  title: Hello\n");
        assertEquals(TaskOutcome.SUCCESS, run().task(":generate").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, run().task(":generate").getOutcome());
        Path source = directory.resolve("build/generated/java/example/app/Values.java"); var modified = Files.getLastModifiedTime(source);
        Files.writeString(yaml, "strings:\n  title: Changed\n");
        assertEquals(TaskOutcome.SUCCESS, run().task(":generate").getOutcome()); assertEquals(modified, Files.getLastModifiedTime(source));
        try (var files = Files.walk(directory.resolve("build/generated"))) { for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file); }
        assertEquals(TaskOutcome.FROM_CACHE, run().task(":generate").getOutcome());
    }
    private BuildResult run() {
        return GradleRunner.create().withProjectDir(directory.toFile()).withPluginClasspath()
                .withArguments("generate", "--build-cache", "--configuration-cache", "--offline", "--stacktrace").build();
    }
}
