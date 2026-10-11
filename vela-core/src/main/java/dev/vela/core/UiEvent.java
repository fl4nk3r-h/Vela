package dev.vela.core;

import java.util.Objects;

public record UiEvent(String name, Object value) {
    public UiEvent {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Event name must not be blank");
        }
    }

    public <T> T valueAs(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return type.cast(value);
    }
}
