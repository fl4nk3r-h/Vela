package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;

public final class VelaButton {
    private VelaButton() {
    }

    public static ViewNode of(String label) {
        return of(label, ComponentVariant.PRIMARY, true, null, Modifier.none());
    }

    public static ViewNode of(String label, Runnable onClick) {
        return of(label, ComponentVariant.PRIMARY, true, onClick, Modifier.none());
    }

    public static ViewNode of(
            String label,
            ComponentVariant variant,
            boolean enabled,
            Runnable onClick,
            Modifier modifier) {
        ViewNode.Builder button = ComponentSupport.node("button", modifier.enabled(enabled).role("button"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("variant", ComponentSupport.enumValue(variant))
                .prop("enabled", enabled);
        if (enabled && onClick != null) {
            button.on("click", event -> onClick.run());
        }
        return button.build();
    }
}
