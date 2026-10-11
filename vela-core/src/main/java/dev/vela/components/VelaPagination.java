package dev.vela.components;

import java.util.Objects;
import java.util.function.IntConsumer;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaPagination {
    private VelaPagination() {
    }

    public static ViewNode of(int currentPage, int pageCount, IntConsumer onPageChange, Modifier modifier) {
        if (pageCount <= 0) {
            throw new IllegalArgumentException("pageCount must be greater than zero");
        }
        if (currentPage < 1 || currentPage > pageCount) {
            throw new IllegalArgumentException("currentPage must be between 1 and pageCount");
        }
        Objects.requireNonNull(onPageChange, "onPageChange");
        return ComponentSupport.node("pagination", modifier.role("navigation"))
                .prop("currentPage", currentPage)
                .prop("pageCount", pageCount)
                .on("change", event -> {
                    double rawPage = ComponentSupport.doubleValue(event.value(), "change");
                    int page = (int) rawPage;
                    if (rawPage != page || page < 1 || page > pageCount) {
                        throw new IllegalArgumentException("Page must be a whole number between 1 and pageCount");
                    }
                    onPageChange.accept(page);
                })
                .build();
    }
}
