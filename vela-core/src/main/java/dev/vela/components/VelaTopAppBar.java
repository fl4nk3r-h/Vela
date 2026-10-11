package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaTopAppBar {
    private VelaTopAppBar() {
    }

    public static ViewNode of(String title, ViewNode navigation, Modifier modifier, ViewNode... actions) {
        ViewNode.Builder appBar = ComponentSupport.node("top-app-bar", modifier)
                .prop("title", ComponentSupport.requireText(title, "title"));
        if (navigation != null) {
            appBar.child(slot("navigation", navigation));
        }
        for (ViewNode action : ComponentSupport.children(actions)) {
            appBar.child(slot("action", action));
        }
        return appBar.build();
    }

    private static ViewNode slot(String name, ViewNode content) {
        return ViewNode.builder("slot").prop("name", name).child(content).build();
    }
}
