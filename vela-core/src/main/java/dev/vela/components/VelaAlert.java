package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;

public final class VelaAlert {
    private VelaAlert() {
    }

    public static ViewNode of(
            String title,
            String message,
            ComponentVariant variant,
            Runnable onDismiss,
            Modifier modifier) {
        ViewNode.Builder alert = ComponentSupport.node("alert", modifier.role("alert"))
                .prop("title", ComponentSupport.requireText(title, "title"))
                .prop("message", ComponentSupport.requireText(message, "message"))
                .prop("variant", ComponentSupport.enumValue(variant));
        if (onDismiss != null) {
            alert.prop("dismissible", true).on("dismiss", event -> onDismiss.run());
        }
        return alert.build();
    }
}
