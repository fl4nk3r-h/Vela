package dev.vela.resources;

import java.text.MessageFormat;
import java.util.*;
import dev.vela.ui.Modifier;

/** A resource environment owned by one runtime, with explicit locale and theme. */
public final class ResourceResolver {
    private final Map<String, String> values;
    private final Locale locale;
    private final String theme;

    public ResourceResolver(ResourceCatalog catalog, Locale locale, String theme) {
        this.locale = Objects.requireNonNull(locale, "locale");
        this.theme = Objects.requireNonNull(theme, "theme");
        values = Objects.requireNonNull(catalog, "catalog").values(locale);
    }
    public static ResourceResolver empty() { return new ResourceResolver(ResourceCatalog.empty(), Locale.ROOT, "default"); }
    public Locale locale() { return locale; }
    public String string(String key, Object... arguments) {
        String value = require("strings." + key);
        return arguments.length == 0 ? value : new MessageFormat(value, locale).format(arguments);
    }
    public String color(String key) { return require("colors." + key); }
    public int dimension(String key) {
        String value = require("dimensions." + key);
        return Integer.parseInt(value.substring(0, value.length() - 2));
    }
    public Modifier style(String key) { return properties("styles." + key + "."); }
    public Modifier theme() { return properties("themes." + theme + "."); }
    public Modifier theme(String name) { return properties("themes." + name + "."); }
    public String value(String key) { return require(key); }

    private Modifier properties(String prefix) {
        Modifier result = Modifier.none();
        for (var entry : values.entrySet()) if (entry.getKey().startsWith(prefix)) {
            result = result.property(entry.getKey().substring(prefix.length()), entry.getValue());
        }
        return result;
    }
    private String require(String key) {
        String value = values.get(key);
        if (value == null) throw new IllegalArgumentException("Unknown Vela resource: " + key);
        return value;
    }
}
