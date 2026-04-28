package dev.vela.components;

import dev.vela.core.ViewNode;

public final class VelaButton {
    private VelaButton() {
    }

    public static ViewNode of(String label) {
        return ViewNode.button(label);
    }
}