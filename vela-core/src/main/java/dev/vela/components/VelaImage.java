package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaImage {
    public enum Scale {
        FIT,
        CROP,
        FILL,
        NONE
    }

    private VelaImage() {
    }

    public static ViewNode of(String source, String contentDescription, Modifier modifier) {
        return of(source, contentDescription, Scale.FIT, modifier);
    }

    public static ViewNode of(String source, String contentDescription, Scale scale, Modifier modifier) {
        return ComponentSupport.node("image", modifier.contentDescription(contentDescription))
                .prop("source", ComponentSupport.requireText(source, "source"))
                .prop("scale", ComponentSupport.enumValue(scale))
                .build();
    }
}
