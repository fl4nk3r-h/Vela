package dev.vela.components;

import java.util.function.IntFunction;

import dev.vela.core.LazyContent;
import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaLazyList {
    private VelaLazyList() { }

    public static ViewNode of(int count, IntFunction<String> key, IntFunction<ViewNode> item, Modifier modifier) {
        return ComponentSupport.node("lazy-list", modifier.role("list"))
                .prop("itemCount", count).lazyContent(new LazyContent(count, key, item)).build();
    }
}
