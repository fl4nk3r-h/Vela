package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaEmptyState {
    private VelaEmptyState() {
    }

    public static ViewNode of(String title, String message, ViewNode illustration, ViewNode action, Modifier modifier) {
        ViewNode.Builder emptyState = ComponentSupport.node("empty-state", modifier)
                .prop("title", ComponentSupport.requireText(title, "title"))
                .prop("message", ComponentSupport.requireText(message, "message"));
        if (illustration != null) {
            emptyState.child(slot("illustration", illustration));
        }
        if (action != null) {
            emptyState.child(slot("action", action));
        }
        return emptyState.build();
    }

    private static ViewNode slot(String name, ViewNode content) {
        return ViewNode.builder("slot").prop("name", name).child(content).build();
    }
}
