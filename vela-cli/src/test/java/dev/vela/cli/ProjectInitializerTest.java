package dev.vela.cli;

import static org.junit.jupiter.api.Assertions.*;
import dev.vela.tooling.*;
import java.nio.file.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectInitializerTest {
    @TempDir Path directory;
    @Test void generatedProjectWithSpacesCompilesWithoutAnnotationsOrHandwrittenXml() throws Exception {
        Path project = directory.resolve("my first app"); new ProjectInitializer().initialize(project, "example.first", null);
        assertEquals("example.first.MainScreen", ProjectConfig.read(project).entry());
        assertTrue(Files.isRegularFile(project.resolve("gradle/wrapper/gradle-wrapper.jar")));
        assertFalse(Files.exists(project.resolve("src/main/AndroidManifest.xml")));
        assertFalse(Files.readString(project.resolve("build.gradle")).contains("annotationProcessor"));
        new ResourceCompiler().generate(project.resolve("src/main/vela"), "example.first", project.resolve("generated"));
        Path output = project.resolve("classes"); Files.createDirectories(output);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-d", output.toString(), "-classpath", System.getProperty("java.class.path"),
                project.resolve("src/main/java/example/first/MainScreen.java").toString(), project.resolve("generated/java/example/first/Values.java").toString());
        assertEquals(0, result);
    }
    @Test void conflictsAndInvalidPackagesLeaveExistingFilesAlone() throws Exception {
        Path existing = directory.resolve("existing"); Files.createDirectories(existing); Files.writeString(existing.resolve("keep"), "unchanged");
        assertThrows(java.io.IOException.class, () -> new ProjectInitializer().initialize(existing, "example.app", null));
        assertEquals("unchanged", Files.readString(existing.resolve("keep")));
        Path invalid = directory.resolve("invalid");
        assertThrows(IllegalArgumentException.class, () -> new ProjectInitializer().initialize(invalid, "example.class", null)); assertFalse(Files.exists(invalid));
    }
    @Test void emptyDestinationAndBundledRepositoryWork() throws Exception {
        Path project = directory.resolve("empty"), repository = directory.resolve("repository");
        Files.createDirectories(project); Files.createDirectories(repository.resolve("dev/vela")); Files.writeString(repository.resolve("dev/vela/artifact.pom"), "metadata");
        new ProjectInitializer().initialize(project, "example.app", repository);
        assertEquals("metadata", Files.readString(project.resolve(".vela/repository/dev/vela/artifact.pom")));
        assertEquals(0, Vela.execute(project, "--help"));
    }
}
