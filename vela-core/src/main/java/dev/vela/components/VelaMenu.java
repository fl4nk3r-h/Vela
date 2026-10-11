package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaMenu {
    private VelaMenu() {
    }

    public static ViewNode of(Modifier modifier, ViewNode... items) {
        return ComponentSupport.node("menu", modifier.role("menu"))
                .children(ComponentSupport.children(items))
                .build();
    }

    public static ViewNode item(String label, String icon, boolean enabled, Runnable onSelect, Modifier modifier) {
        ViewNode.Builder item = ComponentSupport.node("menu-item", modifier.enabled(enabled).role("menuitem"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("enabled", enabled);
        if (icon != null && !icon.isBlank()) {
            item.prop("icon", icon);
        }
        if (enabled && onSelect != null) {
            item.on("select", event -> onSelect.run());
        }
        return item.build();
    }
}
