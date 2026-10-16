package dev.vela.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import dev.vela.components.*;
import dev.vela.ui.*;

class AuditRegressionTest {
    @Test
    void disposalFailureDoesNotAbandonOtherRemovedSiblings() {
        RecordingBackend backend = new RecordingBackend() {
            @Override public void dispose(Handle handle) {
                super.dispose(handle);
                if ("a".equals(handle.node.key())) throw new IllegalStateException("dispose a");
            }
        };
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(ViewNode.column(List.of(ViewNode.text("a").withKey("a"), ViewNode.text("b").withKey("b"))));
            assertThrows(IllegalStateException.class, () -> tree.render(ViewNode.column(List.of())));
            assertEquals(2, backend.disposed);
        }
        assertEquals(backend.created, backend.disposed);
    }

    @Test
    void identicalSubtreesAreSkippedButNewDescriptionsRefreshCallbacks() {
        AtomicInteger updates = new AtomicInteger();
        RecordingBackend backend = new RecordingBackend() {
            @Override public void update(Handle h, ViewNode previous, ViewNode next) {
                updates.incrementAndGet(); super.update(h, previous, next);
            }
        };
        ViewNode stable = ViewNode.column(List.of(ViewNode.text("cached")));
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(ViewNode.column(List.of(stable, ViewNode.text("before"))));
            tree.render(ViewNode.column(List.of(stable, ViewNode.text("after"))));
            assertEquals(2, updates.get());
        }
    }

    @Test
    void disabledInputsCannotDispatchAndEnabledInputsCan() {
        AtomicInteger calls = new AtomicInteger();
        List<ViewNode> disabled = List.of(
                VelaSelect.of("Mode", "a", List.of("a", "b"), v -> calls.incrementAndGet(), Modifier.none().enabled(false)),
                VelaSlider.of("Volume", 0, 0, 10, 2, v -> calls.incrementAndGet(), Modifier.none().enabled(false)),
                VelaTextField.multiline("Notes", "", "", 2, v -> calls.incrementAndGet(), Modifier.none().enabled(false)),
                VelaSegmentedControl.of(List.of("a", "b"), "a", v -> calls.incrementAndGet(), Modifier.none().enabled(false)));
        for (ViewNode node : disabled) assertFalse(node.dispatch("change", "b"));
        assertEquals(0, calls.get());
        assertTrue(VelaSelect.of("Mode", "a", List.of("a", "b"), v -> calls.incrementAndGet(), Modifier.none())
                .dispatch("change", "b"));
        assertEquals(1, calls.get());
    }

    @Test
    void sliderSnapsStepsAndIncludesNonDivisibleMaximum() {
        AtomicReference<Double> selected = new AtomicReference<>();
        ViewNode node = VelaSlider.of("Count", 1.3, 0, 10, 2, selected::set, Modifier.none());
        assertEquals("2.0", node.props().get("value"));
        node.dispatch("change", 1.3);
        assertEquals(2.0, selected.get());
        SliderRange decimals = new SliderRange(-0.2, 0.35, 0.1);
        assertEquals(6, decimals.intervals());
        assertEquals(0.35, decimals.valueAt(6));
        assertEquals(0.1, decimals.snap(0.11));
        assertThrows(IllegalArgumentException.class, () -> node.dispatch("change", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> node.dispatch("change", 11));
        assertThrows(IllegalArgumentException.class, () -> new SliderRange(0, 10, Double.MIN_VALUE));
    }

    @Test
    void protocolTokensDoNotChangeWithTurkishLocale() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("vertical", VelaScroll.of(Orientation.VERTICAL, Modifier.none(), VelaText.of("x"))
                    .props().get("orientation"));
            assertEquals("primary", VelaButton.of("x").props().get("variant"));
            assertEquals("vertical", Modifier.none().scrollable(Orientation.VERTICAL).properties().get("interaction.scrollable"));
        } finally { Locale.setDefault(old); }
    }

    @Test
    void bothDiagnosticRenderersRedactPasswordsAndExplicitSensitiveValues() {
        ViewNode password = VelaTextField.of("Password", "audit-secret", "", VelaTextField.InputType.PASSWORD,
                true, ignored -> { }, Modifier.none());
        ViewNode custom = ViewNode.builder("text").text("custom-secret").prop("value", "custom-secret").sensitive().build();
        ViewNode subtree = ViewNode.builder("column").sensitive().child(ViewNode.text("nested-secret")).build();
        ViewNode root = ViewNode.column(List.of(password, custom, subtree));
        assertFalse(new VelaRenderer().render(root).contains("audit-secret"));
        assertFalse(new VelaRenderer().render(root).contains("custom-secret"));
        assertFalse(new VelaRenderer().render(root).contains("nested-secret"));
        TextRendererBackend backend = new TextRendererBackend();
        try (MountedTree<TextRendererBackend.Node> tree = new MountedTree<>(backend)) {
            tree.render(root);
            assertFalse(backend.output().contains("audit-secret"));
            assertFalse(backend.output().contains("custom-secret"));
            assertTrue(backend.output().contains("<redacted>"));
        }
    }

    @Test
    void typedPropertiesValidateLegacyWireValuesAndLazyFactoriesStayLazy() {
        ViewNode node = ViewNode.builder("text").prop(StandardProps.ENABLED, false).build();
        assertFalse(node.property(StandardProps.ENABLED, true));
        assertThrows(IllegalArgumentException.class, () -> ViewNode.builder("text").prop("state.enabled", "sometimes").build());
        assertThrows(IllegalArgumentException.class, () -> ViewNode.builder("text").prop("style.opacity", "NaN").build());
        AtomicInteger created = new AtomicInteger();
        ViewNode lazy = VelaLazyList.of(10_000, i -> "row-" + i, i -> {
            created.incrementAndGet(); return VelaText.of("Row " + i);
        }, Modifier.none());
        assertEquals(0, created.get());
        assertTrue(lazy.children().isEmpty());
        assertEquals("row-7", lazy.lazyContent().nodeAt(7).key());
        assertEquals(1, created.get());
        assertThrows(IndexOutOfBoundsException.class, () -> lazy.lazyContent().nodeAt(10_000));
        assertEquals(1, created.get());
    }

    @Test
    void childStateUpdatesWithoutRenderingItsParentOrSiblingAndLifecyclesAreScoped() {
        Parent parent = new Parent();
        var frames = new ArrayDeque<Runnable>();
        TextRendererBackend backend = new TextRendererBackend();
        try (VelaRuntime<TextRendererBackend.Node> runtime = new VelaRuntime<>(parent, backend, frames::addLast)) {
            runtime.mount();
            parent.first.value.set(1);
            assertEquals(1, frames.size());
            frames.removeFirst().run();
            assertTrue(backend.output().contains("first=1"));
            assertEquals(1, parent.renders);
            assertEquals(2, parent.first.renders);
            assertEquals(1, parent.second.renders);
            assertEquals(1, parent.first.mounts);
            parent.showFirst.set(false);
            frames.removeFirst().run();
            assertEquals(1, parent.first.unmounts);
            assertEquals(1, parent.first.disposals);
        }
        assertEquals(1, parent.second.unmounts);
        assertEquals(1, parent.second.disposals);
    }

    @Test
    void saveableStateRestoresRootAndChildrenBeforeMount() {
        StateSnapshot snapshot;
        Parent original = new Parent();
        try (var runtime = new VelaRuntime<>(original, new TextRendererBackend(), callback -> { })) {
            runtime.mount();
            original.first.value.set(4);
            snapshot = original.saveState();
        }
        Parent restored = new Parent();
        restored.restoreState(snapshot);
        TextRendererBackend backend = new TextRendererBackend();
        try (var runtime = new VelaRuntime<>(restored, backend, callback -> { })) {
            runtime.mount();
            assertEquals(4, restored.first.value.get());
            assertTrue(backend.output().contains("first=4"));
        }
    }

    @Test
    void restoredChildSnapshotIsConsumedOnceAcrossRemounts() {
        Parent parent = new Parent();
        parent.restoreState(new StateSnapshot(java.util.Map.of(), java.util.Map.of("first",
                new StateSnapshot(java.util.Map.of("value", "4"), java.util.Map.of()))));
        try (var runtime = new VelaRuntime<>(parent, new TextRendererBackend(), callback -> { })) {
            runtime.mount();
            assertEquals(4, parent.first.value.get());
            parent.first.value.set(5);
            parent.showFirst.set(false); runtime.flush();
            parent.showFirst.set(true); runtime.flush();
            assertEquals(5, parent.first.value.get());
            assertEquals(2, parent.first.mounts);
        }
    }

    @Test
    void scopedCleanupContinuesAndReleasesOwnershipAfterFailure() {
        AtomicInteger released = new AtomicInteger();
        VelaComponent component = new VelaComponent() {
            @Override protected void onMount() {
                onDispose(released::incrementAndGet);
                onDispose(() -> { throw new IllegalArgumentException("cancel failed"); });
            }
            @Override public ViewNode render() { return VelaText.of("Mounted"); }
        };
        var first = new VelaRuntime<>(component, new TextRendererBackend(), callback -> { });
        first.mount();
        assertThrows(IllegalStateException.class, first::close);
        assertEquals(1, released.get());
        var second = new VelaRuntime<>(component, new TextRendererBackend(), callback -> { });
        second.mount();
        assertThrows(IllegalStateException.class, second::close);
        assertEquals(2, released.get());
    }

    @Test
    void dispatchedBackgroundResultsRunOnOwnerAndAreDroppedAfterClose() throws Exception {
        Parent parent = new Parent();
        var callbacks = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
        var runtime = new VelaRuntime<>(parent, new TextRendererBackend(), callbacks::add);
        runtime.mount();
        Thread worker = new Thread(() -> runtime.dispatch(() -> parent.first.value.set(3)));
        worker.start(); worker.join();
        assertEquals(0, parent.first.value.get());
        callbacks.remove().run();
        assertEquals(3, parent.first.value.get());
        AtomicInteger late = new AtomicInteger();
        runtime.dispatch(late::incrementAndGet);
        runtime.close();
        runtime.dispatch(late::incrementAndGet);
        callbacks.forEach(Runnable::run);
        assertEquals(0, late.get());
    }

    static final class Child extends VelaComponent {
        final String name;
        final MutableState<Integer> value = saveableState("value", 0, StateCodec.INTEGER);
        int renders, mounts, unmounts, disposals;
        Child(String name) { this.name = name; }
        @Override protected void onMount() { mounts++; onDispose(() -> disposals++); }
        @Override protected void onUnmount() { unmounts++; }
        @Override public ViewNode render() { renders++; return VelaText.of(name + "=" + value.get()); }
    }
    static final class Parent extends VelaComponent {
        final Child first = new Child("first"), second = new Child("second");
        final MutableState<Boolean> showFirst = saveableState("showFirst", true, StateCodec.BOOLEAN);
        int renders;
        @Override public ViewNode render() {
            renders++;
            return VelaColumn.of(showFirst.get() ? child("first", first) : VelaText.of("hidden"), child("second", second));
        }
    }
}
