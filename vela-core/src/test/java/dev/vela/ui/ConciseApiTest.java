package dev.vela.ui;

import static org.junit.jupiter.api.Assertions.*;
import static dev.vela.ui.UI.*;
import dev.vela.core.*;
import dev.vela.resources.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConciseApiTest {
    @Test void bindingsUpdateStateAndHonorDisabledModifiers() {
        Screen screen = new Screen();
        field("Name", screen.name).dispatch("change", "Ada");
        toggle("Enabled", screen.enabled).dispatch("change", true);
        slider("Level", screen.level, SliderOptions.defaults().step(5)).dispatch("change", 12.0);
        assertEquals("Ada", screen.name.get()); assertTrue(screen.enabled.get()); assertEquals(10, screen.level.get());
        var disabled = field("Name", screen.name, FieldOptions.defaults().enabled(false));
        assertFalse(disabled.dispatch("change", "ignored")); assertEquals("Ada", screen.name.get());
    }
    @Test void modifiersCopyDescriptionsWithoutLosingKeysChildrenOrCallbacks() {
        int[] calls = {0}; ViewNode original = button("Go", () -> calls[0]++).withKey("go");
        ViewNode modified = original.with(padding(16));
        assertEquals("go", modified.key()); assertFalse(original.props().containsKey("style.padding"));
        assertEquals("16dp", modified.props().get("style.padding")); modified.dispatch("click"); assertEquals(1, calls[0]);
        assertFalse(modified.with(Modifier.none().enabled(false)).dispatch("click"));
        assertThrows(IllegalArgumentException.class, () -> modified.with(Modifier.none().property("style.opacity", 2)));
        var child = text("child"); assertSame(child, column(child).with(padding(4)).children().get(0));
    }
    @Test void keyedBuiltInStateRoundTripsAndIncompatibleTypesReset() {
        Screen before = new Screen(); before.name.set("Ada"); before.count.set(9);
        Screen after = new Screen(); after.restoreState(before.saveState());
        assertEquals("Ada", after.name.get()); assertEquals(9, after.count.get());
        List<String> diagnostic = new ArrayList<>();
        var changed = new VelaComponent() {
            final MutableState<String> count = state("count", "new");
            @Override public ViewNode render() { return text(count.get()); }
        };
        changed.restoreState(before.saveState(), diagnostic::add);
        assertEquals("new", changed.render().text()); assertEquals(List.of("Reset incompatible state: count"), diagnostic);
    }
    @Test void resourcesAreScopedToRuntimeAndRefreshChildren() {
        var child = new VelaComponent() { public ViewNode render() { return text(resources().string("title")); } };
        var parent = new VelaComponent() { public ViewNode render() { return child("child", child); } };
        Deque<Runnable> queue = new ArrayDeque<>(); TextRendererBackend backend = new TextRendererBackend();
        try (var runtime = new VelaRuntime<>(parent, backend, queue::addLast, resolver("One"))) {
            runtime.mount(); assertTrue(backend.output().contains("One"));
            runtime.updateResources(resolver("Two")); runtime.flush(); assertTrue(backend.output().contains("Two"));
        }
    }
    @Test void namedOptionsKeepIndependentDefaults() {
        var options = ButtonOptions.defaults().variant(ComponentVariant.OUTLINED).enabled(false);
        assertEquals(ComponentVariant.PRIMARY, ButtonOptions.defaults().variant());
        assertFalse(button("Go", () -> fail("disabled"), options).dispatch("click"));
        assertThrows(IllegalArgumentException.class, () -> SliderOptions.defaults().range(2, 1));
    }
    private static ResourceResolver resolver(String title) {
        return new ResourceResolver(new ResourceCatalog(Map.of("", Map.of("strings.title", title))), Locale.ENGLISH, "default");
    }
    private static final class Screen extends VelaComponent {
        final MutableState<String> name = state("name", "");
        final MutableState<Integer> count = state("count", 0);
        final MutableState<Boolean> enabled = state("enabled", false);
        final MutableState<Double> level = state("level", 0.0);
        public ViewNode render() { return column(field("Name", name), toggle("Enabled", enabled)); }
    }
}
