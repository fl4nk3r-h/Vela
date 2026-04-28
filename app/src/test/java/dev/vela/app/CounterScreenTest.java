package dev.vela.app;

import dev.vela.core.VelaRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterScreenTest {
    @Test
    void incrementMarksTheScreenDirtyAndUpdatesTheRenderTree() {
        CounterScreen screen = new CounterScreen();

        screen.increment();

        assertTrue(screen.isDirty());
        assertTrue(new VelaRenderer().render(screen.render()).contains("Current value: 1"));
    }
}