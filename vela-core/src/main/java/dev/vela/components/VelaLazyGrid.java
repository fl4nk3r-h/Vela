package dev.vela.components;

import java.util.function.IntFunction;

import dev.vela.core.LazyContent;
import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaLazyGrid {
    private VelaLazyGrid() { }

    public static ViewNode columns(int columns, int count, IntFunction<String> key,
            IntFunction<ViewNode> item, Modifier modifier) {
        if (columns <= 0) throw new IllegalArgumentException("columns must be positive");
        return ComponentSupport.node("lazy-grid", modifier.role("grid"))
                .prop("layout.columns", columns).prop("itemCount", count)
                .lazyContent(new LazyContent(count, key, item)).build();
    }
}
