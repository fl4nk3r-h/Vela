package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaList {
    private VelaList() {
    }

    public static ViewNode of(boolean divided, Modifier modifier, ViewNode... items) {
        return ComponentSupport.node("list", modifier.role("list"))
                .prop("divided", divided)
                .children(ComponentSupport.children(items))
                .build();
    }
}
