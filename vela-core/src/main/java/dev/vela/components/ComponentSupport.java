package dev.vela.components;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

final class ComponentSupport {
    private ComponentSupport() {
    }

    static ViewNode.Builder node(String type, Modifier modifier) {
        return ViewNode.builder(type).modifier(Objects.requireNonNull(modifier, "modifier"));
    }

    static List<ViewNode> children(ViewNode... children) {
        Objects.requireNonNull(children, "children");
        return List.copyOf(Arrays.asList(children));
    }

    static String enumValue(Enum<?> value) {
        return Objects.requireNonNull(value, "value").name().toLowerCase().replace('_', '-');
    }

    static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    static double requireRange(double value, double minimum, double maximum, String name) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    static String stringValue(Object value, String eventName) {
        if (value == null) {
            throw new IllegalArgumentException(eventName + " requires a value");
        }
        return String.valueOf(value);
    }

    static boolean booleanValue(Object value, String eventName) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue
                && (stringValue.equalsIgnoreCase("true") || stringValue.equalsIgnoreCase("false"))) {
            return Boolean.parseBoolean(stringValue);
        }
        throw new IllegalArgumentException(eventName + " requires a boolean value");
    }

    static double doubleValue(Object value, String eventName) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(eventName + " requires a numeric value", exception);
            }
        }
        throw new IllegalArgumentException(eventName + " requires a numeric value");
    }
}
