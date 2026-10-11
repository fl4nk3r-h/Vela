package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSnackbar {
    private VelaSnackbar() {
    }

    public static ViewNode of(String message, String actionLabel, Runnable onAction, Modifier modifier) {
        ViewNode.Builder snackbar = ComponentSupport.node("snackbar", modifier.role("status"))
                .prop("message", ComponentSupport.requireText(message, "message"));
        if (actionLabel != null && !actionLabel.isBlank()) {
            if (onAction == null) {
                throw new IllegalArgumentException("onAction is required when actionLabel is present");
            }
            snackbar.prop("actionLabel", actionLabel).on("action", event -> onAction.run());
        }
        return snackbar.build();
    }
}
