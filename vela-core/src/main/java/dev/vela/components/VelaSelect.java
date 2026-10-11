package dev.vela.components;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSelect {
    private VelaSelect() {
    }

    public static ViewNode of(
            String label,
            String selectedValue,
            List<String> options,
            Consumer<String> onSelectionChange,
            Modifier modifier) {
        List<String> immutableOptions = List.copyOf(Objects.requireNonNull(options, "options"));
        if (immutableOptions.isEmpty()) {
            throw new IllegalArgumentException("options must not be empty");
        }
        if (!immutableOptions.contains(selectedValue)) {
            throw new IllegalArgumentException("selectedValue must be present in options");
        }
        Objects.requireNonNull(onSelectionChange, "onSelectionChange");
        ViewNode.Builder select = ComponentSupport.node("select", modifier)
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("selectedValue", selectedValue)
                .prop("optionCount", immutableOptions.size())
                .on("change", event -> {
                    String value = ComponentSupport.stringValue(event.value(), "change");
                    if (!immutableOptions.contains(value)) {
                        throw new IllegalArgumentException("Selected value is not an available option: " + value);
                    }
                    onSelectionChange.accept(value);
                });
        immutableOptions.forEach(option -> select.child(ViewNode.builder("option")
                .prop("value", option)
                .prop("selected", option.equals(selectedValue))
                .build()));
        return select.build();
    }
}
