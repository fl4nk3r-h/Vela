package dev.vela.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TextStyle {
    private static final TextStyle DEFAULT = new TextStyle(Map.of());

    private final Map<String, String> properties;

    private TextStyle(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public static TextStyle defaultStyle() {
        return DEFAULT;
    }

    public Map<String, String> properties() {
        return properties;
    }

    public TextStyle fontSize(int sp) {
        if (sp <= 0) {
            throw new IllegalArgumentException("sp must be greater than zero");
        }
        return property("text.fontSize", sp + "sp");
    }

    public TextStyle fontWeight(int weight) {
        if (weight < 100 || weight > 900 || weight % 100 != 0) {
            throw new IllegalArgumentException("weight must be a multiple of 100 between 100 and 900");
        }
        return property("text.fontWeight", weight);
    }

    public TextStyle color(String color) {
        return property("text.color", requireText(color, "color"));
    }

    public TextStyle fontFamily(String family) {
        return property("text.fontFamily", requireText(family, "family"));
    }

    public TextStyle align(Alignment alignment) {
        return property("text.align", Objects.requireNonNull(alignment, "alignment").name().toLowerCase());
    }

    public TextStyle lineHeight(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 0) {
            throw new IllegalArgumentException("multiplier must be greater than zero");
        }
        return property("text.lineHeight", multiplier);
    }

    public TextStyle italic(boolean italic) {
        return property("text.italic", italic);
    }

    public TextStyle underline(boolean underline) {
        return property("text.underline", underline);
    }

    public TextStyle maxLines(int maxLines) {
        if (maxLines <= 0) {
            throw new IllegalArgumentException("maxLines must be greater than zero");
        }
        return property("text.maxLines", maxLines);
    }

    private TextStyle property(String name, Object value) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(properties);
        updated.put(name, String.valueOf(value));
        return new TextStyle(updated);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
