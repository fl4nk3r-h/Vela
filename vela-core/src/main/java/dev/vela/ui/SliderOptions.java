package dev.vela.ui;

import java.util.Objects;

public record SliderOptions(double minimum, double maximum, double step, Modifier modifier) {
    public SliderOptions { new SliderRange(minimum, maximum, step); Objects.requireNonNull(modifier); }
    public static SliderOptions defaults() { return new SliderOptions(0, 100, 1, Modifier.none()); }
    public SliderOptions range(double min, double max) { return new SliderOptions(min, max, step, modifier); }
    public SliderOptions step(double value) { return new SliderOptions(minimum, maximum, value, modifier); }
    public SliderOptions modifier(Modifier value) { return new SliderOptions(minimum, maximum, step, value); }
}
