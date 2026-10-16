package dev.vela.core;

import java.util.List;
import java.util.Map;

/** Shared validated properties used by components and native adapters. */
public final class StandardProps {
    public static final PropKey<Boolean> ENABLED = PropKey.bool("state.enabled");
    public static final PropKey<Boolean> VISIBLE = PropKey.bool("state.visible");
    public static final PropKey<Boolean> FOCUSABLE = PropKey.bool("interaction.focusable");
    public static final PropKey<Boolean> SENSITIVE = PropKey.bool("semantics.sensitive");
    public static final PropKey<String> ROLE = PropKey.text("semantics.role");
    public static final PropKey<Double> OPACITY = PropKey.number("style.opacity");
    public static final PropKey<Double> ASPECT_RATIO = PropKey.number("style.aspectRatio");
    public static final PropKey<Double> WEIGHT = PropKey.number("layout.weight");

    private StandardProps() { }

    static void validate(Map<String, String> props) {
        for (PropKey<Boolean> key : List.of(ENABLED, VISIBLE, FOCUSABLE, SENSITIVE)) key.read(props, true);
        double opacity = OPACITY.read(props, 1.0);
        if (opacity < 0 || opacity > 1) throw new IllegalArgumentException("Opacity outside 0-1");
        if (ASPECT_RATIO.read(props, 1.0) <= 0 || WEIGHT.read(props, 1.0) <= 0) {
            throw new IllegalArgumentException("Aspect ratio and weight must be positive");
        }
    }
}
