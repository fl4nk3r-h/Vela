package dev.vela.core;

import java.util.Objects;
import java.util.function.IntFunction;

/** Describes data without building item nodes. Native adapters materialize visible rows only. */
public record LazyContent(int count, IntFunction<String> key, IntFunction<ViewNode> item) {
    public LazyContent {
        if (count < 0) throw new IllegalArgumentException("count must be nonnegative");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(item, "item");
    }

    public String keyAt(int index) {
        checkIndex(index);
        String value = Objects.requireNonNull(key.apply(index), "item key");
        if (value.isBlank()) throw new IllegalArgumentException("Item key must not be blank");
        return value;
    }

    public ViewNode nodeAt(int index) {
        return Objects.requireNonNull(item.apply(index), "item node").withKey(keyAt(index));
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException(index);
    }
}
