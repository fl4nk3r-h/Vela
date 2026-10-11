package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaAvatar {
    private VelaAvatar() {
    }

    public static ViewNode image(String source, String contentDescription, int sizeDp, Modifier modifier) {
        if (sizeDp <= 0) {
            throw new IllegalArgumentException("sizeDp must be greater than zero");
        }
        return ComponentSupport.node("avatar", modifier.size(sizeDp).contentDescription(contentDescription))
                .prop("source", ComponentSupport.requireText(source, "source"))
                .prop("kind", "image")
                .build();
    }

    public static ViewNode initials(String initials, int sizeDp, Modifier modifier) {
        if (sizeDp <= 0) {
            throw new IllegalArgumentException("sizeDp must be greater than zero");
        }
        return ComponentSupport.node("avatar", modifier.size(sizeDp))
                .prop("initials", ComponentSupport.requireText(initials, "initials"))
                .prop("kind", "initials")
                .build();
    }
}
