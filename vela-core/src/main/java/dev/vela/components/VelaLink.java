package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaLink {
    private VelaLink() {
    }

    public static ViewNode of(String label, String destination, Runnable onOpen, Modifier modifier) {
        Objects.requireNonNull(onOpen, "onOpen");
        return ComponentSupport.node("link", modifier.role("link"))
                .text(ComponentSupport.requireText(label, "label"))
                .prop("label", label)
                .prop("destination", ComponentSupport.requireText(destination, "destination"))
                .on("open", event -> onOpen.run())
                .build();
    }
}
