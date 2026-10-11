package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Alignment;
import dev.vela.ui.Modifier;

public final class VelaBox {
    private VelaBox() {
    }

    public static ViewNode of(ViewNode... children) {
        return of(Alignment.START, Modifier.none(), children);
    }

    public static ViewNode of(Alignment contentAlignment, Modifier modifier, ViewNode... children) {
        return ComponentSupport.node("box", modifier)
                .prop("layout.contentAlignment", ComponentSupport.enumValue(contentAlignment))
                .children(ComponentSupport.children(children))
                .build();
    }
}
