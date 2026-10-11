package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaRadioButton {
    private VelaRadioButton() {
    }

    public static ViewNode of(
            String label,
            String value,
            boolean selected,
            boolean enabled,
            Runnable onSelect,
            Modifier modifier) {
        Objects.requireNonNull(onSelect, "onSelect");
        ViewNode.Builder radio = ComponentSupport.node("radio-button", modifier.enabled(enabled).role("radio"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("value", ComponentSupport.requireText(value, "value"))
                .prop("selected", selected)
                .prop("enabled", enabled);
        if (enabled) {
            radio.on("select", event -> onSelect.run());
        }
        return radio.build();
    }
}
