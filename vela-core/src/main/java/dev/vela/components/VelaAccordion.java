package dev.vela.components;

import java.util.Objects;
import java.util.function.Consumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaAccordion {
    private VelaAccordion() {
    }

    public static ViewNode item(
            String title,
            boolean expanded,
            ViewNode content,
            Consumer<Boolean> onExpandedChange,
            Modifier modifier) {
        Objects.requireNonNull(onExpandedChange, "onExpandedChange");
        return ComponentSupport.node("accordion-item", modifier)
                .prop("title", ComponentSupport.requireText(title, "title"))
                .prop("expanded", expanded)
                .child(Objects.requireNonNull(content, "content"))
                .on("change", event -> onExpandedChange.accept(
                        ComponentSupport.booleanValue(event.value(), "change")))
                .build();
    }

    public static ViewNode group(Modifier modifier, ViewNode... items) {
        return ComponentSupport.node("accordion", modifier)
                .children(ComponentSupport.children(items))
                .build();
    }
}
