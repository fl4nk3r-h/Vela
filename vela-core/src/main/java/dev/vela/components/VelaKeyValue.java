package dev.vela.components;

import java.util.Map;
import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaKeyValue {
    private VelaKeyValue() {
    }

    public static ViewNode of(Map<String, String> entries, Modifier modifier) {
        ViewNode.Builder list = ComponentSupport.node("key-value-list", modifier);
        Objects.requireNonNull(entries, "entries").forEach((key, value) -> list.child(
                ViewNode.builder("key-value-item")
                        .prop("key", ComponentSupport.requireText(key, "key"))
                        .prop("value", Objects.requireNonNull(value, "value"))
                        .build()));
        return list.build();
    }
}
