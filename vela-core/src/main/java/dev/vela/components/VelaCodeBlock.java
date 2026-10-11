package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaCodeBlock {
    private VelaCodeBlock() {
    }

    public static ViewNode of(String code, String language, Runnable onCopy, Modifier modifier) {
        ViewNode.Builder block = ComponentSupport.node("code-block", modifier)
                .text(Objects.requireNonNull(code, "code"))
                .prop("language", Objects.requireNonNullElse(language, "text"));
        if (onCopy != null) {
            block.on("copy", event -> onCopy.run());
        }
        return block.build();
    }
}
