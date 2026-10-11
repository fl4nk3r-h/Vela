package dev.vela.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Modifier {
    private static final Modifier NONE = new Modifier(Map.of());

    private final Map<String, String> properties;

    private Modifier(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public static Modifier none() {
        return NONE;
    }

    public Map<String, String> properties() {
        return properties;
    }

    public Modifier then(Modifier other) {
        Objects.requireNonNull(other, "other");
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(properties);
        merged.putAll(other.properties);
        return new Modifier(merged);
    }

    public Modifier property(String name, Object value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Property name must not be blank");
        }
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(properties);
        updated.put(name, String.valueOf(value));
        return new Modifier(updated);
    }

    public Modifier width(int dp) {
        return dimension("style.width", dp);
    }

    public Modifier height(int dp) {
        return dimension("style.height", dp);
    }

    public Modifier size(int dp) {
        return width(dp).height(dp);
    }

    public Modifier minWidth(int dp) {
        return dimension("style.minWidth", dp);
    }

    public Modifier maxWidth(int dp) {
        return dimension("style.maxWidth", dp);
    }

    public Modifier minHeight(int dp) {
        return dimension("style.minHeight", dp);
    }

    public Modifier maxHeight(int dp) {
        return dimension("style.maxHeight", dp);
    }

    public Modifier fillMaxWidth() {
        return property("style.width", "100%");
    }

    public Modifier fillMaxHeight() {
        return property("style.height", "100%");
    }

    public Modifier padding(int dp) {
        return dimension("style.padding", dp);
    }

    public Modifier padding(int verticalDp, int horizontalDp) {
        requireNonNegative(verticalDp, "verticalDp");
        requireNonNegative(horizontalDp, "horizontalDp");
        return property("style.padding", verticalDp + "dp " + horizontalDp + "dp");
    }

    public Modifier margin(int dp) {
        return dimension("style.margin", dp);
    }

    public Modifier background(String color) {
        return property("style.background", requireText(color, "color"));
    }

    public Modifier foreground(String color) {
        return property("style.foreground", requireText(color, "color"));
    }

    public Modifier border(int widthDp, String color) {
        requireNonNegative(widthDp, "widthDp");
        return property("style.border", widthDp + "dp solid " + requireText(color, "color"));
    }

    public Modifier cornerRadius(int dp) {
        return dimension("style.cornerRadius", dp);
    }

    public Modifier shadow(int elevationDp) {
        return dimension("style.shadow", elevationDp);
    }

    public Modifier opacity(double opacity) {
        if (!Double.isFinite(opacity) || opacity < 0 || opacity > 1) {
            throw new IllegalArgumentException("opacity must be between 0 and 1");
        }
        return property("style.opacity", opacity);
    }

    public Modifier aspectRatio(double ratio) {
        if (!Double.isFinite(ratio) || ratio <= 0) {
            throw new IllegalArgumentException("ratio must be greater than zero");
        }
        return property("style.aspectRatio", ratio);
    }

    public Modifier weight(double weight) {
        if (!Double.isFinite(weight) || weight <= 0) {
            throw new IllegalArgumentException("weight must be greater than zero");
        }
        return property("layout.weight", weight);
    }

    public Modifier alignSelf(Alignment alignment) {
        return property("layout.alignSelf", enumValue(alignment));
    }

    public Modifier zIndex(int zIndex) {
        return property("layout.zIndex", zIndex);
    }

    public Modifier visible(boolean visible) {
        return property("state.visible", visible);
    }

    public Modifier enabled(boolean enabled) {
        return property("state.enabled", enabled);
    }

    public Modifier focusable(boolean focusable) {
        return property("interaction.focusable", focusable);
    }

    public Modifier scrollable(Orientation orientation) {
        return property("interaction.scrollable", enumValue(orientation));
    }

    public Modifier contentDescription(String description) {
        return property("semantics.contentDescription", requireText(description, "description"));
    }

    public Modifier role(String role) {
        return property("semantics.role", requireText(role, "role"));
    }

    public Modifier testTag(String tag) {
        return property("semantics.testTag", requireText(tag, "tag"));
    }

    private Modifier dimension(String property, int dp) {
        requireNonNegative(dp, "dp");
        return property(property, dp + "dp");
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String enumValue(Enum<?> value) {
        return Objects.requireNonNull(value, "value").name().toLowerCase().replace('_', '-');
    }
}
