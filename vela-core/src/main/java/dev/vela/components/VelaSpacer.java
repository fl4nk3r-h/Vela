package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSpacer {
    private VelaSpacer() {
    }

    public static ViewNode size(int widthDp, int heightDp) {
        return ComponentSupport.node("spacer", Modifier.none().width(widthDp).height(heightDp)).build();
    }

    public static ViewNode horizontal(int widthDp) {
        return size(widthDp, 0);
    }

    public static ViewNode vertical(int heightDp) {
        return size(0, heightDp);
    }
}
