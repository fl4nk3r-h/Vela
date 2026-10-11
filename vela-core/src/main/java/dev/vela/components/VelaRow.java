package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Alignment;
import dev.vela.ui.Arrangement;
import dev.vela.ui.Modifier;

public final class VelaRow {
    private VelaRow() {
    }

    public static ViewNode of(ViewNode... children) {
        return of(Arrangement.START, Alignment.CENTER, 0, Modifier.none(), children);
    }

    public static ViewNode of(Modifier modifier, ViewNode... children) {
        return of(Arrangement.START, Alignment.CENTER, 0, modifier, children);
    }

    public static ViewNode of(
            Arrangement arrangement,
            Alignment alignment,
            int gapDp,
            Modifier modifier,
            ViewNode... children) {
        return ComponentSupport.node("row", modifier)
                .prop("layout.arrangement", ComponentSupport.enumValue(arrangement))
                .prop("layout.alignment", ComponentSupport.enumValue(alignment))
                .prop("layout.gap", ComponentSupport.requireNonNegative(gapDp, "gapDp") + "dp")
                .children(ComponentSupport.children(children))
                .build();
    }
}
