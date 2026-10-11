package dev.vela.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;

import org.junit.jupiter.api.Test;

import dev.vela.core.TextRendererBackend;
import dev.vela.core.VelaRenderer;
import dev.vela.core.VelaRuntime;

class CounterScreenTest {
    @Test
    void incrementMarksTheScreenDirtyAndUpdatesTheRenderTree() {
        CounterScreen screen = new CounterScreen();

        boolean handled = screen.render().children().get(2).dispatch("click");

        assertTrue(handled);
        assertTrue(screen.isDirty());
        assertTrue(new VelaRenderer().render(screen.render()).contains("Current value: 1"));
    }

    @Test
    void mountedCounterBatchesClicksAndRetainsItsButton() {
        CounterScreen screen = new CounterScreen();
        TextRendererBackend backend = new TextRendererBackend();
        ArrayDeque<Runnable> frames = new ArrayDeque<>();
        TextRendererBackend.Node button;
        try (VelaRuntime<TextRendererBackend.Node> runtime = new VelaRuntime<>(screen, backend, frames::addLast)) {
            runtime.mount();
            button = runtime.root().children().get(2);
            button.dispatch("click");
            button.dispatch("click");
            assertEquals(1, frames.size());
            frames.removeFirst().run();
            assertSame(button, runtime.root().children().get(2));
            assertTrue(backend.output().contains("Current value: 2"));
            assertFalse(screen.isDirty());
        }
        assertFalse(button.dispatch("click"));
        assertEquals(2, screen.count());
    }

    @Test
    void resettingAnAlreadyZeroCounterDoesNotInvalidate() {
        CounterScreen screen = new CounterScreen();
        screen.reset();
        assertFalse(screen.isDirty());
    }
}
