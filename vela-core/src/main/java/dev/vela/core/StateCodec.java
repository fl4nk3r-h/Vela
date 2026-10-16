package dev.vela.core;

import java.util.Objects;
import java.util.function.Function;

/** Explicit serialization for saveable state; no reflection or Java deserialization. */
public record StateCodec<T>(Function<T, String> encode, Function<String, T> decode) {
    public static final StateCodec<String> STRING = new StateCodec<>(Function.identity(), Function.identity());
    public static final StateCodec<Integer> INTEGER = new StateCodec<>(Object::toString, Integer::valueOf);
    public static final StateCodec<Long> LONG = new StateCodec<>(Object::toString, Long::valueOf);
    public static final StateCodec<Double> DOUBLE = new StateCodec<>(Object::toString, value -> {
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) throw new IllegalArgumentException("State must be finite");
        return parsed;
    });
    public static final StateCodec<Boolean> BOOLEAN = new StateCodec<>(Object::toString, value -> {
        if (!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException("Invalid boolean state");
        return Boolean.valueOf(value);
    });

    public StateCodec {
        Objects.requireNonNull(encode, "encode");
        Objects.requireNonNull(decode, "decode");
    }
}
