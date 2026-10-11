package dev.vela.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import dev.vela.ui.Modifier;

public final class ViewNode {
    private final String type;
    private final String key;
    private final String text;
    private final Map<String, String> props;
    private final List<ViewNode> children;
    private final Map<String, UiEventHandler> eventHandlers;

    private ViewNode(
            String type,
            String key,
            String text,
            Map<String, String> props,
            List<ViewNode> children,
            Map<String, UiEventHandler> eventHandlers) {
        this.type = Objects.requireNonNull(type, "type");
        this.key = key;
        this.text = text;
        this.props = Collections.unmodifiableMap(new LinkedHashMap<>(props));
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
        this.eventHandlers = Collections.unmodifiableMap(new LinkedHashMap<>(eventHandlers));
    }

    public static ViewNode text(String value) {
        return builder("text").text(value).prop("value", value).build();
    }

    public static ViewNode button(String label) {
        return builder("button").prop("label", label).build();
    }

    public static ViewNode column(List<ViewNode> children) {
        return builder("column").children(children).build();
    }

    public static ViewNode node(String type, Map<String, String> props, List<ViewNode> children) {
        return builder(type).props(props).children(children).build();
    }

    public static Builder builder(String type) {
        return new Builder(type);
    }

    public String type() {
        return type;
    }

    public String key() {
        return key;
    }

    public ViewNode withKey(String key) {
        return new ViewNode(
                type,
                Builder.requireText(key, "key"),
                text,
                props,
                children,
                eventHandlers);
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

    public Set<String> eventNames() {
        return eventHandlers.keySet();
    }

    public boolean dispatch(String eventName) {
        return dispatch(eventName, null);
    }

    public boolean dispatch(String eventName, Object value) {
        Objects.requireNonNull(eventName, "eventName");
        UiEventHandler handler = eventHandlers.get(eventName);
        if (handler == null) {
            return false;
        }
        handler.handle(new UiEvent(eventName, value));
        return true;
    }

    public static final class Builder {
        private final String type;
        private final Map<String, String> props = new LinkedHashMap<>();
        private final List<ViewNode> children = new ArrayList<>();
        private final Map<String, UiEventHandler> eventHandlers = new LinkedHashMap<>();
        private String key;
        private String text;

        private Builder(String type) {
            this.type = requireText(type, "type");
        }

        public Builder key(String key) {
            this.key = requireText(key, "key");
            return this;
        }

        public Builder text(String text) {
            this.text = Objects.requireNonNull(text, "text");
            return this;
        }

        public Builder prop(String name, Object value) {
            props.put(requireText(name, "name"), String.valueOf(Objects.requireNonNull(value, "value")));
            return this;
        }

        public Builder props(Map<String, String> props) {
            Objects.requireNonNull(props, "props").forEach(this::prop);
            return this;
        }

        public Builder modifier(Modifier modifier) {
            props.putAll(Objects.requireNonNull(modifier, "modifier").properties());
            return this;
        }

        public Builder child(ViewNode child) {
            children.add(Objects.requireNonNull(child, "child"));
            return this;
        }

        public Builder children(List<ViewNode> children) {
            Objects.requireNonNull(children, "children").forEach(this::child);
            return this;
        }

        public Builder on(String eventName, UiEventHandler handler) {
            eventHandlers.put(requireText(eventName, "eventName"), Objects.requireNonNull(handler, "handler"));
            return this;
        }

        public ViewNode build() {
            return new ViewNode(type, key, text, props, children, eventHandlers);
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
