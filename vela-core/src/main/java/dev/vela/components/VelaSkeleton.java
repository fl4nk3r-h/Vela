package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSkeleton {
    public enum Shape {
        RECTANGLE,
        ROUNDED,
        CIRCLE,
        TEXT
    }

    private VelaSkeleton() {
    }

    public static ViewNode of(Shape shape, Modifier modifier) {
        return ComponentSupport.node("skeleton", modifier)
                .prop("shape", ComponentSupport.enumValue(shape))
                .prop("loading", true)
                .build();
    }
}
