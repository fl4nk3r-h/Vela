package dev.vela.components;

import java.util.Objects;
import java.util.function.DoubleConsumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSlider {
    private VelaSlider() {
    }

    public static ViewNode of(
            String label,
            double value,
            double minimum,
            double maximum,
            double step,
            DoubleConsumer onValueChange,
            Modifier modifier) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || maximum <= minimum) {
            throw new IllegalArgumentException("maximum must be greater than minimum");
        }
        ComponentSupport.requireRange(value, minimum, maximum, "value");
        if (!Double.isFinite(step) || step <= 0 || step > maximum - minimum) {
            throw new IllegalArgumentException("step must be positive and no larger than the range");
        }
        Objects.requireNonNull(onValueChange, "onValueChange");
        return ComponentSupport.node("slider", modifier.role("slider"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("value", value)
                .prop("minimum", minimum)
                .prop("maximum", maximum)
                .prop("step", step)
                .on("change", event -> onValueChange.accept(ComponentSupport.requireRange(
                        ComponentSupport.doubleValue(event.value(), "change"), minimum, maximum, "value")))
                .build();
    }
}
