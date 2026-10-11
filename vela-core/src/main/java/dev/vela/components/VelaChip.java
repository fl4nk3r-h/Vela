package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;

public final class VelaChip {
    private VelaChip() {
    }

    public static ViewNode of(String label, ComponentVariant variant, Runnable onClick, Modifier modifier) {
        ViewNode.Builder chip = ComponentSupport.node("chip", modifier.role("button"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("variant", ComponentSupport.enumValue(variant));
        if (onClick != null) {
            chip.on("click", event -> onClick.run());
        }
        return chip.build();
    }

    public static ViewNode removable(
            String label,
            ComponentVariant variant,
            Runnable onClick,
            Runnable onRemove,
            Modifier modifier) {
        Objects.requireNonNull(onRemove, "onRemove");
        return ComponentSupport.node("chip", modifier.role("button"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("variant", ComponentSupport.enumValue(variant))
                .prop("removable", true)
                .on("click", event -> {
                    if (onClick != null) {
                        onClick.run();
                    }
                })
                .on("remove", event -> onRemove.run())
                .build();
    }
}
