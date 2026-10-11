package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;

public final class VelaFloatingActionButton {
    private VelaFloatingActionButton() {
    }

    public static ViewNode of(
            String icon,
            String contentDescription,
            ComponentVariant variant,
            Runnable onClick,
            Modifier modifier) {
        Objects.requireNonNull(onClick, "onClick");
        return ComponentSupport.node(
                "floating-action-button",
                modifier.role("button").contentDescription(contentDescription))
                .prop("icon", ComponentSupport.requireText(icon, "icon"))
                .prop("variant", ComponentSupport.enumValue(variant))
                .on("click", event -> onClick.run())
                .build();
    }
}
