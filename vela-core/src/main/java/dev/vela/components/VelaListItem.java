package dev.vela.components;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaListItem {
    private VelaListItem() {
    }

    public static ViewNode of(
            String headline,
            String supportingText,
            ViewNode leading,
            ViewNode trailing,
            Runnable onClick,
            Modifier modifier) {
        ViewNode.Builder item = ComponentSupport.node("list-item", modifier)
                .prop("headline", ComponentSupport.requireText(headline, "headline"));
        if (supportingText != null && !supportingText.isBlank()) {
            item.prop("supportingText", supportingText);
        }
        if (leading != null) {
            item.child(slot("leading", leading));
        }
        if (trailing != null) {
            item.child(slot("trailing", trailing));
        }
        if (onClick != null) {
            item.on("click", event -> onClick.run());
        }
        return item.build();
    }

    private static ViewNode slot(String name, ViewNode content) {
        return ViewNode.builder("slot").prop("name", name).child(content).build();
    }
}
