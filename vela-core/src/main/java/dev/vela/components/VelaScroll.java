package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;
import dev.vela.ui.Orientation;

public final class VelaScroll {
    private VelaScroll() {
    }

    public static ViewNode of(Orientation orientation, Modifier modifier, ViewNode child) {
        return ComponentSupport.node("scroll", modifier.scrollable(orientation))
                .prop("orientation", ComponentSupport.enumValue(orientation))
                .child(Objects.requireNonNull(child, "child"))
                .build();
    }
}
