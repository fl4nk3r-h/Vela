package dev.vela.android;

import static dev.vela.ui.UI.*;
import dev.vela.core.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class HostTestActivity extends VelaActivity {
    public static final AtomicInteger active = new AtomicInteger();
    public static final AtomicInteger disposed = new AtomicInteger();
    @Override protected VelaComponent createScreen() { return new Screen("Initial"); }
    public static final class Screen extends VelaComponent {
        private final String title;
        final MutableState<Integer> count = state("count", 0);
        final MutableState<String> name = state("name", "");
        public Screen(String title) { this.title = title; }
        @Override protected void onMount() { active.incrementAndGet(); onDispose(() -> { active.decrementAndGet(); disposed.incrementAndGet(); }); }
        @Override public ViewNode render() {
            if (title.equals("Broken")) throw new IllegalStateException("Broken candidate");
            return column(text(title + " " + count.get()), button("Increment", () -> count.update(n -> n + 1)), field("Name", name).withKey("name"));
        }
    }
}
