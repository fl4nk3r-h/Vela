package dev.vela.components;

import java.util.Objects;
import java.util.function.Consumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaSwitch {
    private VelaSwitch() {
    }

    public static ViewNode of(
            String label,
            boolean checked,
            boolean enabled,
            Consumer<Boolean> onCheckedChange,
            Modifier modifier) {
        Objects.requireNonNull(onCheckedChange, "onCheckedChange");
        return ComponentSupport.node("switch", modifier.enabled(enabled).role("switch"))
                .prop("label", ComponentSupport.requireText(label, "label"))
                .prop("checked", checked)
                .prop("enabled", enabled)
                .on("change", event -> {
                    if (enabled) {
                        onCheckedChange.accept(ComponentSupport.booleanValue(event.value(), "change"));
                    }
                })
                .build();
    }
}
