package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaNavigationBar {
    private VelaNavigationBar() {
    }

    public static ViewNode of(Modifier modifier, ViewNode... items) {
        return ComponentSupport.node("navigation-bar", modifier.role("navigation"))
                .children(ComponentSupport.children(items))
                .build();
    }

    public static ViewNode item(String label, String icon, boolean selected, Runnable onSelect, Modifier modifier) {
        ViewNode.Builder item = ComponentSupport.node("navigation-item", modifier)
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("icon", ComponentSupport.requireText(icon, "icon"))
                .prop("selected", selected);
        if (onSelect != null) {
            item.on("select", event -> onSelect.run());
        }
        return item.build();
    }
}
