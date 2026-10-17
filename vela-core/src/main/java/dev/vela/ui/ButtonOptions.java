package dev.vela.ui;

import java.util.Objects;

public record ButtonOptions(ComponentVariant variant, boolean enabled, Modifier modifier) {
    public ButtonOptions { Objects.requireNonNull(variant); Objects.requireNonNull(modifier); }
    public static ButtonOptions defaults() { return new ButtonOptions(ComponentVariant.PRIMARY, true, Modifier.none()); }
    public ButtonOptions variant(ComponentVariant value) { return new ButtonOptions(value, enabled, modifier); }
    public ButtonOptions enabled(boolean value) { return new ButtonOptions(variant, value, modifier); }
    public ButtonOptions modifier(Modifier value) { return new ButtonOptions(variant, enabled, value); }
}
