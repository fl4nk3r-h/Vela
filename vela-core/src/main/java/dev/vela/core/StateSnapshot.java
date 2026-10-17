package dev.vela.core;

import java.util.Map;

/** Portable snapshot of explicitly saveable values and keyed child scopes. */
public record StateSnapshot(Map<String, String> values, Map<String, StateSnapshot> children,
                            Map<String, String> codecs) {
    public StateSnapshot(Map<String, String> values, Map<String, StateSnapshot> children) {
        this(values, children, Map.of());
    }
    public StateSnapshot {
        values = Map.copyOf(values);
        children = Map.copyOf(children);
        codecs = Map.copyOf(codecs);
    }
}
