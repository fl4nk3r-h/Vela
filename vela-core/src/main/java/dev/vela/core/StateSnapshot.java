package dev.vela.core;

import java.util.Map;

/** Portable snapshot of explicitly saveable values and keyed child scopes. */
public record StateSnapshot(Map<String, String> values, Map<String, StateSnapshot> children) {
    public StateSnapshot {
        values = Map.copyOf(values);
        children = Map.copyOf(children);
    }
}
