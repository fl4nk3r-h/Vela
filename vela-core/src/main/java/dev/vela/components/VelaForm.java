package dev.vela.components;

import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaForm {
    private VelaForm() {
    }

    public static ViewNode of(String name, Runnable onSubmit, Modifier modifier, ViewNode... fields) {
        Objects.requireNonNull(onSubmit, "onSubmit");
        return ComponentSupport.node("form", modifier.role("form"))
                .prop("name", ComponentSupport.requireText(name, "name"))
                .children(ComponentSupport.children(fields))
                .on("submit", event -> onSubmit.run())
                .build();
    }
}
