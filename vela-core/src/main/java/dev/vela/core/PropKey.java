package dev.vela.core;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** A typed property accessor preserving the string wire format for existing backends. */
public record PropKey<T>(String name, Function<String, T> decode, Function<T, String> encode) {
    public PropKey {
        if (Objects.requireNonNull(name, "name").isBlank()) throw new IllegalArgumentException("Blank property name");
        Objects.requireNonNull(decode, "decode");
        Objects.requireNonNull(encode, "encode");
    }

    public T read(Map<String, String> props, T fallback) {
        String value = props.get(name);
        return value == null ? fallback : decode.apply(value);
    }

    public static PropKey<Boolean> bool(String name) {
        return new PropKey<>(name, value -> {
            if (!"true".equals(value) && !"false".equals(value)) {
                throw new IllegalArgumentException(name + " requires true or false");
            }
            return Boolean.valueOf(value);
        }, Object::toString);
    }

    public static PropKey<Double> number(String name) {
        return new PropKey<>(name, value -> {
            double number = Double.parseDouble(value);
            if (!Double.isFinite(number)) throw new IllegalArgumentException(name + " must be finite");
            return number;
        }, value -> {
            if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
            return value.toString();
        });
    }

    public static PropKey<String> text(String name) {
        return new PropKey<>(name, Function.identity(), Function.identity());
    }
}
