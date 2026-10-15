package dev.vela.android;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import dev.vela.core.FrameScheduler;

/** Thread-safe posting to the UI frame loop, with physical cancellation on close. */
public final class AndroidFrameScheduler implements FrameScheduler, AutoCloseable {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object token = new Object();
    private final Set<Choreographer.FrameCallback> frames = new HashSet<>();
    private volatile boolean closed;

    @Override public synchronized void schedule(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        if (closed) throw new IllegalStateException("Scheduler is closed");
        handler.postAtTime(() -> {
            if (closed) return;
            Choreographer.FrameCallback frame = new Choreographer.FrameCallback() {
                @Override public void doFrame(long time) {
                    frames.remove(this);
                    if (!closed) callback.run();
                }
            };
            frames.add(frame);
            Choreographer.getInstance().postFrameCallback(frame);
        }, token, SystemClock.uptimeMillis());
    }

    @Override public synchronized void close() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Close on the UI thread");
        closed = true;
        handler.removeCallbacksAndMessages(token);
        for (Choreographer.FrameCallback frame : frames) Choreographer.getInstance().removeFrameCallback(frame);
        frames.clear();
    }
}
