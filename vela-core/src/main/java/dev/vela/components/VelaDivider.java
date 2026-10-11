package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;
import dev.vela.ui.Orientation;

public final class VelaDivider {
    private VelaDivider() {
    }

    public static ViewNode horizontal() {
        return of(Orientation.HORIZONTAL, 1, Modifier.none());
    }

    public static ViewNode of(Orientation orientation, int thicknessDp, Modifier modifier) {
        if (thicknessDp <= 0) {
            throw new IllegalArgumentException("thicknessDp must be greater than zero");
        }
        return ComponentSupport.node("divider", modifier)
                .prop("orientation", ComponentSupport.enumValue(orientation))
                .prop("thickness", thicknessDp + "dp")
                .build();
    }
}
