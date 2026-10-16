package dev.vela.android;

import android.graphics.drawable.Drawable;
import java.util.function.Consumer;

/** Loaders deliver results on the UI thread and return a cancellation handle. */
@FunctionalInterface
public interface ImageLoader {
    AutoCloseable load(String source, Consumer<Drawable> result, Consumer<Exception> failure);
}
