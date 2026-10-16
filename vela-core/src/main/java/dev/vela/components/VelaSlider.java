package dev.vela.components;

import java.util.Objects;
import java.util.function.DoubleConsumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;
import dev.vela.ui.SliderRange;

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
        SliderRange range = new SliderRange(minimum, maximum, step);
        ComponentSupport.requireRange(value, minimum, maximum, "value");
        Objects.requireNonNull(onValueChange, "onValueChange");
        return ComponentSupport.node("slider", modifier.role("slider"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("value", range.snap(value))
                .prop("minimum", minimum)
                .prop("maximum", maximum)
                .prop("step", step)
                .on("change", event -> onValueChange.accept(range.snap(
                        ComponentSupport.doubleValue(event.value(), "change"))))
                .build();
    }
}
