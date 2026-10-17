package dev.vela.dev;

import java.util.*;

/** Application definitions and their compiler/D8 nested synthetic classes only. */
public final class ReloadClasses {
    private final Set<String> definitions;
    public ReloadClasses(Collection<String> definitions) { this.definitions = Set.copyOf(definitions); }
    public boolean owns(String name) {
        if (definitions.contains(name)) return true;
        int nested = name.indexOf('$');
        while (nested >= 0) {
            if (definitions.contains(name.substring(0, nested))) return true;
            nested = name.indexOf('$', nested + 1);
        }
        return false;
    }
}
