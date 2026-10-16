package dev.vela.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A finite stepped range; the maximum is always a selectable endpoint. */
public record SliderRange(double minimum, double maximum, double step) {
    public SliderRange {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)
                || !Double.isFinite(maximum - minimum) || maximum <= minimum) {
            throw new IllegalArgumentException("maximum must be finite and greater than minimum");
        }
        if (!Double.isFinite(step) || step <= 0 || step > maximum - minimum) {
            throw new IllegalArgumentException("step must be positive and no larger than the range");
        }
        try {
            intervalCount(minimum, maximum, step);
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("Range has too many steps", failure);
        }
    }

    public int intervals() {
        return intervalCount(minimum, maximum, step);
    }

    public double valueAt(int index) {
        if (index < 0 || index > intervals()) {
            throw new IllegalArgumentException("Step index outside range");
        }
        return BigDecimal.valueOf(minimum).add(BigDecimal.valueOf(step).multiply(BigDecimal.valueOf(index)))
                .min(BigDecimal.valueOf(maximum)).doubleValue();
    }

    public int indexOf(double value) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException("Value outside range");
        }
        int lower = BigDecimal.valueOf(value).subtract(BigDecimal.valueOf(minimum))
                .divide(BigDecimal.valueOf(step), 0, RoundingMode.FLOOR).intValueExact();
        int upper = Math.min(intervals(), lower + 1);
        return value - valueAt(lower) < valueAt(upper) - value ? lower : upper;
    }

    public double snap(double value) {
        return valueAt(indexOf(value));
    }

    private static int intervalCount(double minimum, double maximum, double step) {
        return BigDecimal.valueOf(maximum).subtract(BigDecimal.valueOf(minimum))
                .divide(BigDecimal.valueOf(step), 0, RoundingMode.CEILING).intValueExact();
    }
}
