package dev.vela.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class VelaRuntimeTest {
    @Test
    void observableChangesCoalesceAndEqualWritesDoNotInvalidate() {
        Screen screen = new Screen();
        RecordingBackend backend = new RecordingBackend();
        Deque<Runnable> frames = new ArrayDeque<>();
        try (VelaRuntime<RecordingBackend.Handle> runtime = new VelaRuntime<>(screen, backend, frames::addLast)) {
            runtime.mount();
            var root = runtime.root();
            assertFalse(screen.count.set(0));
            assertTrue(frames.isEmpty());
            screen.count.update(value -> value + 1);
            screen.count.update(value -> value + 1);
            assertEquals(1, frames.size());
            assertEquals(1, screen.renders);
            frames.removeFirst().run();
            assertEquals(2, screen.renders);
            assertSame(root, runtime.root());
            assertEquals("2", root.node.text());
            assertFalse(screen.isDirty());
        }
        assertEquals(1, screen.mounts);
        assertEquals(1, screen.unmounts);
    }

    @Test
    void flushInvalidatesTheOldQueuedCallback() {
        Screen screen = new Screen();
        Deque<Runnable> frames = new ArrayDeque<>();
        try (VelaRuntime<RecordingBackend.Handle> runtime = new VelaRuntime<>(screen, new RecordingBackend(), frames::addLast)) {
            runtime.mount();
            screen.count.set(1);
            Runnable stale = frames.removeFirst();
            runtime.flush();
            screen.count.set(2);
            stale.run();
            assertEquals(2, screen.renders);
            frames.removeFirst().run();
            assertEquals(3, screen.renders);
        }
    }

    @Test
    void closingDropsQueuedWorkAndAllowsAComponentToBeMountedAgain() {
        Screen screen = new Screen();
        RecordingBackend backend = new RecordingBackend();
        Deque<Runnable> frames = new ArrayDeque<>();
        VelaRuntime<RecordingBackend.Handle> runtime = new VelaRuntime<>(screen, backend, frames::addLast);
        runtime.mount();
        screen.count.set(1);
        runtime.close();
        runtime.close();
        frames.removeFirst().run();
        assertEquals(1, screen.renders);
        assertEquals(1, backend.disposed);
        assertNull(backend.root);
        assertThrows(IllegalStateException.class, runtime::mount);
        try (VelaRuntime<RecordingBackend.Handle> next = new VelaRuntime<>(screen, new RecordingBackend(), frames::addLast)) {
            next.mount();
            assertEquals(2, screen.mounts);
            assertEquals(2, screen.renders);
        }
    }

    @Test
    void rejectsWrongThreadStateChangesBeforeMutatingTheValue() throws InterruptedException {
        Screen screen = new Screen();
        Deque<Runnable> frames = new ArrayDeque<>();
        try (VelaRuntime<RecordingBackend.Handle> runtime = new VelaRuntime<>(screen, new RecordingBackend(), frames::addLast)) {
            runtime.mount();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    screen.count.set(2);
                } catch (Throwable error) {
                    failure.set(error);
                }
            });
            thread.start();
            thread.join();
            assertInstanceOf(IllegalStateException.class, failure.get());
            assertEquals(0, screen.count.get());
            assertTrue(frames.isEmpty());
        }
    }

    @Test
    void preventsDoubleMountWithoutDetachingTheFirstRuntime() {
        Screen screen = new Screen();
        Deque<Runnable> frames = new ArrayDeque<>();
        try (VelaRuntime<RecordingBackend.Handle> first = new VelaRuntime<>(screen, new RecordingBackend(), frames::addLast);
                VelaRuntime<RecordingBackend.Handle> second = new VelaRuntime<>(screen, new RecordingBackend(), frames::addLast)) {
            first.mount();
            assertThrows(IllegalStateException.class, second::mount);
            screen.count.set(1);
            frames.removeFirst().run();
            assertEquals(2, screen.renders);
        }
    }

    @Test
    void rejectsAnInlineSchedulerWithoutRecursivelyRendering() {
        Screen screen = new Screen();
        try (VelaRuntime<RecordingBackend.Handle> runtime = new VelaRuntime<>(screen, new RecordingBackend(), Runnable::run)) {
            runtime.mount();
            assertThrows(IllegalStateException.class, () -> screen.count.set(1));
            assertEquals(1, screen.renders);
            assertTrue(screen.isDirty());
            runtime.flush();
            assertEquals(2, screen.renders);
        }
    }

    @Test
    void failedInitialRenderReleasesTheComponentAndRunsUnmount() {
        Screen screen = new Screen();
        screen.fail = true;
        RecordingBackend backend = new RecordingBackend();
        VelaRuntime<RecordingBackend.Handle> runtime = new VelaRuntime<>(screen, backend, callback -> { });
        assertThrows(IllegalArgumentException.class, runtime::mount);
        assertEquals(1, screen.unmounts);
        assertNull(backend.root);
        screen.fail = false;
        try (VelaRuntime<RecordingBackend.Handle> retry = new VelaRuntime<>(screen, new RecordingBackend(), callback -> { })) {
            retry.mount();
        }
    }

    @Test
    void failedUpdateKeepsDirtyStateUntilAnExplicitRetry() {
        Screen screen = new Screen();
        Deque<Runnable> frames = new ArrayDeque<>();
        try (VelaRuntime<RecordingBackend.Handle> runtime = new VelaRuntime<>(screen, new RecordingBackend(), frames::addLast)) {
            runtime.mount();
            screen.fail = true;
            screen.count.set(1);
            assertThrows(IllegalArgumentException.class, () -> frames.removeFirst().run());
            assertTrue(screen.isDirty());
            assertTrue(frames.isEmpty());
            screen.fail = false;
            runtime.requestRender();
            frames.removeFirst().run();
            assertEquals("1", runtime.root().node.text());
            assertFalse(screen.isDirty());
        }
    }

    private static final class Screen extends VelaComponent {
        private final MutableState<Integer> count = state(0);
        private int renders;
        private int mounts;
        private int unmounts;
        private boolean fail;

        @Override protected void onMount() { mounts++; }
        @Override protected void onUnmount() { unmounts++; }

        @Override
        public ViewNode render() {
            renders++;
            if (fail) {
                throw new IllegalArgumentException("Render failed");
            }
            return ViewNode.text(Integer.toString(count.get()));
        }
    }
}
