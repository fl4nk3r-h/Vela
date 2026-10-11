package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;
import dev.vela.ui.TextStyle;

public final class VelaText {
    private VelaText() {
    }

    public static ViewNode of(String value) {
        return of(value, TextStyle.defaultStyle(), Modifier.none());
    }

    public static ViewNode of(String value, Modifier modifier) {
        return of(value, TextStyle.defaultStyle(), modifier);
    }

    public static ViewNode of(String value, TextStyle style, Modifier modifier) {
        return ComponentSupport.node("text", modifier)
                .text(value)
                .prop("value", value)
                .props(style.properties())
                .build();
    }
}
