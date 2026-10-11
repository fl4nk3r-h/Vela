package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;

public final class VelaIconButton {
    private VelaIconButton() {
    }

    public static ViewNode of(
            String icon,
            String contentDescription,
            ComponentVariant variant,
            boolean enabled,
            Runnable onClick,
            Modifier modifier) {
        Objects.requireNonNull(onClick, "onClick");
        ViewNode.Builder button = ComponentSupport.node(
                "icon-button",
                modifier.enabled(enabled).role("button").contentDescription(contentDescription))
                .prop("icon", ComponentSupport.requireText(icon, "icon"))
                .prop("variant", ComponentSupport.enumValue(variant))
                .prop("enabled", enabled);
        if (enabled) {
            button.on("click", event -> onClick.run());
        }
        return button.build();
    }
}
