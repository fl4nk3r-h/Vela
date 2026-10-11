package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaTabs {
    private VelaTabs() {
    }

    public static ViewNode of(Modifier modifier, ViewNode... tabs) {
        return ComponentSupport.node("tabs", modifier.role("tablist"))
                .children(ComponentSupport.children(tabs))
                .build();
    }

    public static ViewNode tab(String label, boolean selected, Runnable onSelect, Modifier modifier) {
        ViewNode.Builder tab = ComponentSupport.node("tab", modifier.role("tab"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("selected", selected);
        if (onSelect != null) {
            tab.on("select", event -> onSelect.run());
        }
        return tab.build();
    }
}
