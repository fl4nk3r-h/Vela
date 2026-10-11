package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaScaffold {
    private VelaScaffold() {
    }

    public static ViewNode of(ViewNode topBar, ViewNode content, ViewNode bottomBar, Modifier modifier) {
        ViewNode.Builder scaffold = ComponentSupport.node("scaffold", modifier);
        if (topBar != null) {
            scaffold.child(slot("top-bar", topBar));
        }
        scaffold.child(slot("content", Objects.requireNonNull(content, "content")));
        if (bottomBar != null) {
            scaffold.child(slot("bottom-bar", bottomBar));
        }
        return scaffold.build();
    }

    private static ViewNode slot(String name, ViewNode content) {
        return ViewNode.builder("slot").prop("name", name).child(content).build();
    }
}
