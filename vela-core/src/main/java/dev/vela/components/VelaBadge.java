package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;

public final class VelaBadge {
    private VelaBadge() {
    }

    public static ViewNode of(String label) {
        return of(label, ComponentVariant.PRIMARY, Modifier.none());
    }

    public static ViewNode of(String label, ComponentVariant variant, Modifier modifier) {
        return ComponentSupport.node("badge", modifier)
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("variant", ComponentSupport.enumValue(variant))
                .build();
    }
}
