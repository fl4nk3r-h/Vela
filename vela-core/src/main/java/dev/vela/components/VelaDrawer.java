package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaDrawer {
    public enum Edge {
        START,
        END
    }

    private VelaDrawer() {
    }

    public static ViewNode of(Edge edge, ViewNode content, Runnable onDismiss, Modifier modifier) {
        Objects.requireNonNull(onDismiss, "onDismiss");
        return ComponentSupport.node("drawer", modifier.role("dialog"))
                .prop("edge", ComponentSupport.enumValue(edge))
                .child(Objects.requireNonNull(content, "content"))
                .on("dismiss", event -> onDismiss.run())
                .build();
    }
}
