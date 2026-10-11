package dev.vela.components;

import java.util.Objects;
import java.util.function.Consumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaTextField {
    public enum InputType {
        TEXT,
        EMAIL,
        PASSWORD,
        NUMBER,
        SEARCH,
        TELEPHONE,
        URL
    }

    private VelaTextField() {
    }

    public static ViewNode of(
            String label,
            String value,
            String placeholder,
            InputType inputType,
            boolean enabled,
            Consumer<String> onValueChange,
            Modifier modifier) {
        Objects.requireNonNull(onValueChange, "onValueChange");
        return ComponentSupport.node("text-field", modifier.enabled(enabled))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("value", Objects.requireNonNull(value, "value"))
                .prop("placeholder", Objects.requireNonNullElse(placeholder, ""))
                .prop("inputType", ComponentSupport.enumValue(inputType))
                .prop("enabled", enabled)
                .prop("multiline", false)
                .on("change", event -> {
                    if (enabled) {
                        onValueChange.accept(ComponentSupport.stringValue(event.value(), "change"));
                    }
                })
                .build();
    }

    public static ViewNode multiline(
            String label,
            String value,
            String placeholder,
            int minimumLines,
            Consumer<String> onValueChange,
            Modifier modifier) {
        if (minimumLines <= 0) {
            throw new IllegalArgumentException("minimumLines must be greater than zero");
        }
        Objects.requireNonNull(onValueChange, "onValueChange");
        return ComponentSupport.node("text-field", modifier)
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("value", Objects.requireNonNull(value, "value"))
                .prop("placeholder", Objects.requireNonNullElse(placeholder, ""))
                .prop("inputType", "text")
                .prop("multiline", true)
                .prop("minimumLines", minimumLines)
                .on("change", event -> onValueChange.accept(ComponentSupport.stringValue(event.value(), "change")))
                .build();
    }
}
