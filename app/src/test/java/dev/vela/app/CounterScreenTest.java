package dev.vela.app;

import dev.vela.core.VelaRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterScreenTest {
    @Test
    void incrementMarksTheScreenDirtyAndUpdatesTheRenderTree() {
        CounterScreen screen = new CounterScreen();

        boolean handled = screen.render().children().get(2).dispatch("click");

        assertTrue(handled);
        assertTrue(screen.isDirty());
        assertTrue(new VelaRenderer().render(screen.render()).contains("Current value: 1"));
    }
}
