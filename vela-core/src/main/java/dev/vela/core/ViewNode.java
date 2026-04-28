package dev.vela.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ViewNode {
    private final String type;
    private final String text;
    private final Map<String, String> props;
    private final List<ViewNode> children;

    private ViewNode(String type, String text, Map<String, String> props, List<ViewNode> children) {
        this.type = Objects.requireNonNull(type, "type");
        this.text = text;
        this.props = Collections.unmodifiableMap(new LinkedHashMap<>(props));
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public static ViewNode text(String value) {
        return new ViewNode("text", value, Map.of("value", value), List.of());
    }

    public static ViewNode button(String label) {
        return new ViewNode("button", null, Map.of("label", label), List.of());
    }

    public static ViewNode column(List<ViewNode> children) {
        return new ViewNode("column", null, Map.of(), children);
    }

    public static ViewNode node(String type, Map<String, String> props, List<ViewNode> children) {
        return new ViewNode(type, null, props, children);
    }

    public String type() {
        return type;
    }

    public String text() {
        return text;
    }

    public Map<String, String> props() {
        return props;
    }

    public List<ViewNode> children() {
        return children;
    }
}