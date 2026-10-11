package dev.vela.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import dev.vela.core.ViewNode;
import dev.vela.ui.Alignment;
import dev.vela.ui.Arrangement;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;
import dev.vela.ui.TextStyle;

class ComponentCatalogTest {
    @Test
    void composesRichLayoutAndContentNodes() {
        ViewNode screen = VelaColumn.of(
                Arrangement.SPACE_BETWEEN,
                Alignment.STRETCH,
                16,
                Modifier.none().fillMaxWidth().padding(24),
                VelaText.of(
                        "Dashboard",
                        TextStyle.defaultStyle().fontSize(28).fontWeight(700),
                        Modifier.none()),
                VelaGrid.columns(
                        2,
                        12,
                        Modifier.none(),
                        VelaCard.of(VelaBadge.of("New")),
                        VelaCard.of(VelaImage.of("hero.png", "Product preview", Modifier.none()))),
                VelaList.of(
                        true,
                        Modifier.none(),
                        VelaListItem.of("Account", "Profile settings", null, null, null, Modifier.none())));

        assertEquals("column", screen.type());
        assertEquals("16dp", screen.props().get("layout.gap"));
        assertEquals("grid", screen.children().get(1).type());
        assertEquals("list", screen.children().get(2).type());
    }

    @Test
    void dispatchesTypedInputEvents() {
        AtomicReference<String> text = new AtomicReference<>();
        AtomicReference<Boolean> checked = new AtomicReference<>();
        AtomicReference<Double> progress = new AtomicReference<>();
        AtomicReference<LocalDate> date = new AtomicReference<>();

        ViewNode field = VelaTextField.of(
                "Email", "", "name@example.com", VelaTextField.InputType.EMAIL, true, text::set, Modifier.none());
        ViewNode checkbox = VelaCheckbox.of("Remember me", false, true, checked::set, Modifier.none());
        ViewNode slider = VelaSlider.of("Volume", 0.5, 0, 1, 0.1, progress::set, Modifier.none());
        ViewNode picker = VelaDatePicker.of(
                "Start date",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2027, 1, 1),
                date::set,
                Modifier.none());

        field.dispatch("change", "vela@example.com");
        checkbox.dispatch("change", true);
        slider.dispatch("change", 0.8);
        picker.dispatch("change", "2026-10-11");

        assertEquals("vela@example.com", text.get());
        assertTrue(checked.get());
        assertEquals(0.8, progress.get());
        assertEquals(LocalDate.of(2026, 10, 11), date.get());
        assertEquals(2, VelaSelect.of(
                "Mode", "a", List.of("a", "b"), ignored -> {
                }, Modifier.none()).children().size());
    }

    @Test
    void composesNavigationFeedbackOverlaysAndData() {
        AtomicBoolean selected = new AtomicBoolean();
        ViewNode navigation = VelaNavigationBar.of(
                Modifier.none(),
                VelaNavigationBar.item("Home", "home", true, () -> selected.set(true), Modifier.none()));
        ViewNode content = VelaTable.of(
                List.of("Name", "Status"),
                List.of(List.of("Build", "Passing")),
                Modifier.none());
        ViewNode scaffold = VelaScaffold.of(
                VelaTopAppBar.of("Vela", null, Modifier.none()),
                VelaColumn.of(
                        VelaAlert.of("Ready", "Everything is working", ComponentVariant.SUCCESS, null, Modifier.none()),
                        content,
                        VelaProgressIndicator.determinate(
                                0.75, VelaProgressIndicator.Style.LINEAR, Modifier.none()),
                        VelaKeyValue.of(Map.of("Version", "0.1.0"), Modifier.none())),
                navigation,
                Modifier.none());

        assertEquals("scaffold", scaffold.type());
        assertEquals(3, scaffold.children().size());
        navigation.children().getFirst().dispatch("select");
        assertTrue(selected.get());
    }

    @Test
    void validatesInvalidComponentConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> VelaGrid.columns(0, 8, Modifier.none()));
        assertThrows(IllegalArgumentException.class,
                () -> VelaSelect.of("Mode", "missing", List.of("a", "b"), value -> {
                }, Modifier.none()));
        assertThrows(IllegalArgumentException.class,
                () -> VelaTable.of(List.of("One", "Two"), List.of(List.of("only one")), Modifier.none()));
    }
}
