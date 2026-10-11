package dev.vela.components;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSegmentedControl {
    private VelaSegmentedControl() {
    }

    public static ViewNode of(
            List<String> options,
            String selectedValue,
            Consumer<String> onSelectionChange,
            Modifier modifier) {
        List<String> immutableOptions = List.copyOf(Objects.requireNonNull(options, "options"));
        if (immutableOptions.size() < 2) {
            throw new IllegalArgumentException("A segmented control requires at least two options");
        }
        if (!immutableOptions.contains(selectedValue)) {
            throw new IllegalArgumentException("selectedValue must be present in options");
        }
        Objects.requireNonNull(onSelectionChange, "onSelectionChange");
        ViewNode.Builder control = ComponentSupport.node("segmented-control", modifier)
                .prop("selectedValue", selectedValue)
                .on("change", event -> {
                    String value = ComponentSupport.stringValue(event.value(), "change");
                    if (!immutableOptions.contains(value)) {
                        throw new IllegalArgumentException("Selected value is not an available option: " + value);
                    }
                    onSelectionChange.accept(value);
                });
        immutableOptions.forEach(option -> control.child(ViewNode.builder("segment")
                .prop("value", option)
                .prop("selected", option.equals(selectedValue))
                .build()));
        return control.build();
    }
}
