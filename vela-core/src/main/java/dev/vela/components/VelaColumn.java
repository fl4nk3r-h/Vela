package dev.vela.components;

import java.util.Arrays;
import java.util.List;

import dev.vela.core.ViewNode;

public final class VelaColumn {
    private VelaColumn() {
    }

    public static ViewNode of(ViewNode... children) {
        return ViewNode.column(List.copyOf(Arrays.asList(children)));
    }
}