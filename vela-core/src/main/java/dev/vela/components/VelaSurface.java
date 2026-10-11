package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSurface {
    private VelaSurface() {
    }

    public static ViewNode of(Modifier modifier, ViewNode... children) {
        return ComponentSupport.node("surface", modifier)
                .children(ComponentSupport.children(children))
                .build();
    }
}
