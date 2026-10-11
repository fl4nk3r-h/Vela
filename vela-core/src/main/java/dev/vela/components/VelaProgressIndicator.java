package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaProgressIndicator {
    public enum Style {
        LINEAR,
        CIRCULAR
    }

    private VelaProgressIndicator() {
    }

    public static ViewNode determinate(double progress, Style style, Modifier modifier) {
        return ComponentSupport.node("progress", modifier.role("progressbar"))
                .prop("style", ComponentSupport.enumValue(style))
                .prop("indeterminate", false)
                .prop("progress", ComponentSupport.requireRange(progress, 0, 1, "progress"))
                .build();
    }

    public static ViewNode indeterminate(Style style, Modifier modifier) {
        return ComponentSupport.node("progress", modifier.role("progressbar"))
                .prop("style", ComponentSupport.enumValue(style))
                .prop("indeterminate", true)
                .build();
    }
}
