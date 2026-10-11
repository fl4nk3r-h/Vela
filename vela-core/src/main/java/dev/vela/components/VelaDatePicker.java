package dev.vela.components;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaDatePicker {
    private VelaDatePicker() {
    }

    public static ViewNode of(
            String label,
            LocalDate value,
            LocalDate minimum,
            LocalDate maximum,
            Consumer<LocalDate> onDateChange,
            Modifier modifier) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(onDateChange, "onDateChange");
        if (minimum != null && maximum != null && maximum.isBefore(minimum)) {
            throw new IllegalArgumentException("maximum must not be before minimum");
        }
        if (minimum != null && value.isBefore(minimum)) {
            throw new IllegalArgumentException("value must not be before minimum");
        }
        if (maximum != null && value.isAfter(maximum)) {
            throw new IllegalArgumentException("value must not be after maximum");
        }

        ViewNode.Builder picker = ComponentSupport.node("date-picker", modifier)
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("value", value);
        if (minimum != null) {
            picker.prop("minimum", minimum);
        }
        if (maximum != null) {
            picker.prop("maximum", maximum);
        }
        return picker.on("change", event -> {
            LocalDate selected = event.value() instanceof LocalDate date
                    ? date
                    : LocalDate.parse(ComponentSupport.stringValue(event.value(), "change"));
            if (minimum != null && selected.isBefore(minimum) || maximum != null && selected.isAfter(maximum)) {
                throw new IllegalArgumentException("Selected date is outside the allowed range");
            }
            onDateChange.accept(selected);
        }).build();
    }
}
