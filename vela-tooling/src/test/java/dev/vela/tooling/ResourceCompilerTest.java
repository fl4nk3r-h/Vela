package dev.vela.tooling;

import static org.junit.jupiter.api.Assertions.*;
import dev.vela.resources.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ResourceCompilerTest {
    @TempDir Path directory;
    private final ResourceCompiler compiler = new ResourceCompiler();
    private static final String BASE = """
            strings:
              title: Hello
              count: '{0,number,integer} items'
            colors:
              primary: '#123456'
            dimensions:
              spacing: 12dp
            styles:
              card:
                padding: '@dimensions.spacing'
                background: '@colors.primary'
            themes:
              default:
                background: '@colors.primary'
            """;
    @Test void typedSourceCompilesAndCatalogResolvesLocalesStylesAndThemes() throws Exception {
        Files.writeString(directory.resolve("values.yaml"), BASE);
        Files.writeString(directory.resolve("values-fr.yaml"), "strings:\n  count: '{0,number,integer} articles'\n");
        var result = compiler.compile(directory, "example.app");
        var resolver = new ResourceResolver(ResourceCatalog.decode(result.catalog().encode()), Locale.FRANCE, "default");
        assertEquals("3 articles", resolver.string("count", 3)); assertEquals("Hello", resolver.string("title"));
        assertEquals("#123456", resolver.color("primary")); assertEquals(12, resolver.dimension("spacing"));
        assertEquals("12dp", resolver.style("card").properties().get("style.padding"));
        assertEquals("#123456", resolver.theme().properties().get("style.background"));
        Path out = directory.resolve("out"); compiler.generate(directory, "example.app", out);
        Path source = out.resolve("java/example/app/Values.java");
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-classpath", System.getProperty("java.class.path"), source.toString());
        assertEquals(0, status, result.source());
        assertTrue(Files.readString(out.resolve("res/values/vela_values.xml")).contains("vela_primary"));
    }
    @Test void valueOnlyEditsLeaveJavaSourceUntouchedAndEncodingIsDeterministic() throws Exception {
        Path input = directory.resolve("values.yaml"); Files.writeString(input, BASE);
        Path out = directory.resolve("output"); compiler.generate(directory, "example.app", out);
        Path source = out.resolve("java/example/app/Values.java"), catalog = out.resolve("assets/vela/values.bin");
        byte[] original = Files.readAllBytes(catalog); var modified = Files.getLastModifiedTime(source);
        compiler.generate(directory, "example.app", out); assertArrayEquals(original, Files.readAllBytes(catalog));
        Files.writeString(input, BASE.replace("Hello", "Changed").replace("#123456", "#654321"));
        compiler.generate(directory, "example.app", out);
        assertEquals(modified, Files.getLastModifiedTime(source)); assertFalse(Arrays.equals(original, Files.readAllBytes(catalog)));
        Files.writeString(input, BASE.replace("title: Hello", "title: Hello\n  extra: Added"));
        compiler.generate(directory, "example.app", out); assertTrue(Files.readString(source).contains("extra()"));
    }
    @Test void invalidYamlHasSourceLocations() throws Exception {
        for (String text : List.of("strings:\n  title: One\n  title: Two\n", "colors:\n  broken: red\n", "dimensions:\n  spacing: 2px\n",
                "colors:\n  a: '@colors.b'\n  b: '@colors.a'\n", "colors:\n  a: '@colors.missing'\n", "styles:\n  bad:\n    unknown: 1\n")) {
            Files.writeString(directory.resolve("values.yaml"), text);
            var error = assertThrows(IllegalArgumentException.class, () -> compiler.compile(directory, "example.app"));
            assertTrue(error.getMessage().matches("(?s).*values.yaml:[0-9]+:.*"), error.getMessage());
        }
    }
    @Test void translationsMustHaveMatchingFormatArguments() throws Exception {
        Files.writeString(directory.resolve("values.yaml"), BASE);
        Files.writeString(directory.resolve("values-fr.yaml"), "strings:\n  count: '{0} articles'\n");
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(directory, "example.app"));
        Files.writeString(directory.resolve("values-fr.yaml"), "strings:\n  unknown: Missing\n");
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(directory, "example.app"));
    }
    @Test void obsoleteLocaleOutputsAreRemoved() throws Exception {
        Files.writeString(directory.resolve("values.yaml"), BASE);
        Path french = directory.resolve("values-fr.yaml"); Files.writeString(french, "strings:\n  title: Bonjour\n");
        Path out = directory.resolve("output"); compiler.generate(directory, "example.app", out);
        Path generated = out.resolve("res/values-b+fr/vela_values.xml"); assertTrue(Files.exists(generated));
        Files.delete(french); compiler.generate(directory, "example.app", out); assertFalse(Files.exists(generated));
    }
    @Test void catalogRejectsTruncationAndTrailingData() throws Exception {
        byte[] bytes = ResourceCatalog.empty().encode();
        assertThrows(java.io.IOException.class, () -> ResourceCatalog.decode(Arrays.copyOf(bytes, 5)));
        assertThrows(java.io.IOException.class, () -> ResourceCatalog.decode(Arrays.copyOf(bytes, bytes.length + 1)));
    }
    @Test void excessiveReferenceChainsProduceDiagnosticsInsteadOfStackOverflow() throws Exception {
        StringBuilder yaml = new StringBuilder("strings:\n");
        for (int i = 0; i < 300; i++) yaml.append("  a").append(String.format("%03d", i)).append(": '@strings.a").append(String.format("%03d", i + 1)).append("'\n");
        yaml.append("  a300: End\n"); Files.writeString(directory.resolve("values.yaml"), yaml);
        var error = assertThrows(IllegalArgumentException.class, () -> compiler.compile(directory, "example.app"));
        assertTrue(error.getMessage().contains("reference chain exceeds"));
    }
}
