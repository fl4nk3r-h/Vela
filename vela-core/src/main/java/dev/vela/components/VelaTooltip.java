package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaTooltip {
    private VelaTooltip() {
    }

    public static ViewNode of(String message, ViewNode anchor, Modifier modifier) {
        return ComponentSupport.node("tooltip", modifier.role("tooltip"))
                .prop("message", ComponentSupport.requireText(message, "message"))
                .child(Objects.requireNonNull(anchor, "anchor"))
                .build();
    }
}
