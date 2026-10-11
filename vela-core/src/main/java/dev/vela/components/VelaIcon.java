package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaIcon {
    private VelaIcon() {
    }

    public static ViewNode of(String name, String contentDescription) {
        return of(name, contentDescription, Modifier.none());
    }

    public static ViewNode of(String name, String contentDescription, Modifier modifier) {
        return ComponentSupport.node("icon", modifier.contentDescription(contentDescription))
                .prop("name", ComponentSupport.requireText(name, "name"))
                .build();
    }
}
