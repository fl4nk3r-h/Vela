package dev.vela.ui;

import java.util.Objects;
import dev.vela.components.VelaTextField.InputType;

public record FieldOptions(String placeholder, InputType inputType, boolean enabled, Modifier modifier) {
    public FieldOptions { Objects.requireNonNull(placeholder); Objects.requireNonNull(inputType); Objects.requireNonNull(modifier); }
    public static FieldOptions defaults() { return new FieldOptions("", InputType.TEXT, true, Modifier.none()); }
    public FieldOptions placeholder(String value) { return new FieldOptions(value, inputType, enabled, modifier); }
    public FieldOptions inputType(InputType value) { return new FieldOptions(placeholder, value, enabled, modifier); }
    public FieldOptions enabled(boolean value) { return new FieldOptions(placeholder, inputType, value, modifier); }
    public FieldOptions modifier(Modifier value) { return new FieldOptions(placeholder, inputType, enabled, value); }
}
