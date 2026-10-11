package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;

public final class VelaCard {
    private VelaCard() {
    }

    public static ViewNode of(ViewNode... children) {
        return of(ComponentVariant.OUTLINED, Modifier.none(), children);
    }

    public static ViewNode of(ComponentVariant variant, Modifier modifier, ViewNode... children) {
        return ComponentSupport.node("card", modifier)
                .prop("variant", ComponentSupport.enumValue(variant))
                .children(ComponentSupport.children(children))
                .build();
    }
}
