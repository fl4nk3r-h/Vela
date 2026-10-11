package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaDialog {
    private VelaDialog() {
    }

    public static ViewNode of(
            String title,
            ViewNode content,
            ViewNode actions,
            Runnable onDismiss,
            Modifier modifier) {
        Objects.requireNonNull(onDismiss, "onDismiss");
        ViewNode.Builder dialog = ComponentSupport.node("dialog", modifier.role("dialog"))
                .prop("title", ComponentSupport.requireText(title, "title"))
                .child(slot("content", Objects.requireNonNull(content, "content")))
                .on("dismiss", event -> onDismiss.run());
        if (actions != null) {
            dialog.child(slot("actions", actions));
        }
        return dialog.build();
    }

    private static ViewNode slot(String name, ViewNode content) {
        return ViewNode.builder("slot").prop("name", name).child(content).build();
    }
}
