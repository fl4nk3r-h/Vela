package dev.vela.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

class StateProcessorTest {
    @Test void supportsTheRunningCompilerVersion() {
        org.junit.jupiter.api.Assertions.assertEquals(javax.lang.model.SourceVersion.latestSupported(), new StateProcessor().getSupportedSourceVersion());
    }
    @Test
    void acceptsMutableStateOnAVelaComponent() {
        CompilationResult result = compile("example.ValidComponent", """
                package example;

                import dev.vela.annotation.State;
                import dev.vela.core.VelaComponent;
                import dev.vela.core.ViewNode;

                final class ValidComponent extends VelaComponent {
                    @State private int count;

                    @Override
                    public ViewNode render() {
                        return ViewNode.text(Integer.toString(count));
                    }
                }
                """);

        assertTrue(result.success(), result::diagnostics);
    }

    @Test
    void acceptsStateOnAnIndirectVelaComponentSubclass() {
        CompilationResult result = compile("example.ChildComponent", """
                package example;

                import dev.vela.annotation.State;
                import dev.vela.core.VelaComponent;
                import dev.vela.core.ViewNode;

                abstract class BaseComponent extends VelaComponent {
                }

                final class ChildComponent extends BaseComponent {
                    @State private String label = "Ready";

                    @Override
                    public ViewNode render() {
                        return ViewNode.text(label);
                    }
                }
                """);

        assertTrue(result.success(), result::diagnostics);
    }

    @Test
    void rejectsFinalState() {
        CompilationResult result = compile("example.FinalStateComponent", """
                package example;

                import dev.vela.annotation.State;
                import dev.vela.core.VelaComponent;
                import dev.vela.core.ViewNode;

                final class FinalStateComponent extends VelaComponent {
                    @State private final int count = 0;

                    @Override
                    public ViewNode render() {
                        return ViewNode.text(Integer.toString(count));
                    }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.diagnostics().contains("@State fields must be mutable"), result::diagnostics);
    }

    @Test
    void rejectsStateOutsideAVelaComponent() {
        CompilationResult result = compile("example.PlainObject", """
                package example;

                import dev.vela.annotation.State;

                final class PlainObject {
                    @State private int count;
                }
                """);

        assertFalse(result.success());
        assertTrue(
                result.diagnostics().contains("@State fields must be declared inside a VelaComponent"),
                result::diagnostics);
    }

    private CompilationResult compile(String className, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            JavaFileObject sourceFile = new SourceFile(className, source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of("-proc:only", "-classpath", System.getProperty("java.class.path")),
                    null,
                    List.of(sourceFile));
            task.setProcessors(List.of(new StateProcessor()));

            boolean success = task.call();
            return new CompilationResult(success, formatDiagnostics(diagnostics));
        } catch (Exception exception) {
            throw new AssertionError("Could not run test compilation", exception);
        }
    }

    private String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        return diagnostics.getDiagnostics().stream()
                .map(diagnostic -> diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("No diagnostics");
    }

    private record CompilationResult(boolean success, String diagnostics) {
    }

    private static final class SourceFile extends SimpleJavaFileObject {
        private final String source;

        private SourceFile(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
