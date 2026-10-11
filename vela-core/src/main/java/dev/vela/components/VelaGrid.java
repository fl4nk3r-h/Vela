package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaGrid {
    private VelaGrid() {
    }

    public static ViewNode columns(int columns, int gapDp, Modifier modifier, ViewNode... children) {
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be greater than zero");
        }
        return ComponentSupport.node("grid", modifier)
                .prop("layout.columns", columns)
                .prop("layout.gap", ComponentSupport.requireNonNegative(gapDp, "gapDp") + "dp")
                .children(ComponentSupport.children(children))
                .build();
    }
}
