package dev.vela.components;

import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaBreadcrumbs {
    private VelaBreadcrumbs() {
    }

    public static ViewNode of(List<String> labels, IntConsumer onNavigate, Modifier modifier) {
        List<String> immutableLabels = List.copyOf(Objects.requireNonNull(labels, "labels"));
        if (immutableLabels.isEmpty()) {
            throw new IllegalArgumentException("labels must not be empty");
        }
        Objects.requireNonNull(onNavigate, "onNavigate");
        ViewNode.Builder breadcrumbs = ComponentSupport.node("breadcrumbs", modifier.role("navigation"));
        for (int index = 0; index < immutableLabels.size(); index++) {
            int itemIndex = index;
            breadcrumbs.child(ComponentSupport.node("breadcrumb", Modifier.none())
                    .prop("label", ComponentSupport.requireText(immutableLabels.get(index), "label"))
                    .prop("index", index)
                    .prop("current", index == immutableLabels.size() - 1)
                    .on("navigate", event -> onNavigate.accept(itemIndex))
                    .build());
        }
        return breadcrumbs.build();
    }
}
