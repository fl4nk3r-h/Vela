package dev.vela.components;

import dev.vela.core.ViewNode;

public final class VelaText {
    private VelaText() {
    }

    public static ViewNode of(String value) {
        return ViewNode.text(value);
    }
}