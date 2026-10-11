package dev.vela.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class MountedTreeTest {
    @Test
    void retainsHandlesAndLocalViewStateWhenPropertiesChange() {
        RecordingBackend backend = new RecordingBackend();
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(column(text("field", "before")));
            RecordingBackend.Handle root = tree.root();
            RecordingBackend.Handle field = root.children.getFirst();
            field.selectionOffset = 4;
            int childUpdates = backend.childUpdates;

            tree.render(column(text("field", "after")));

            assertSame(root, tree.root());
            assertSame(field, root.children.getFirst());
            assertEquals(4, field.selectionOffset);
            assertEquals("after", field.node.text());
            assertEquals(2, backend.created);
            assertEquals(childUpdates, backend.childUpdates);
        }
        assertEquals(2, backend.disposed);
    }

    @Test
    void reordersInsertsAndRemovesKeyedChildrenWithoutRecreatingSurvivors() {
        RecordingBackend backend = new RecordingBackend();
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(column(text("a", "A"), text("b", "B"), text("c", "C")));
            var a = tree.root().children.get(0);
            var c = tree.root().children.get(2);

            tree.render(column(text("c", "C"), text("new", "New"), text("a", "A")));

            assertSame(c, tree.root().children.get(0));
            assertSame(a, tree.root().children.get(2));
            assertEquals(5, backend.created);
            assertEquals(1, backend.disposed);
        }
        assertEquals(5, backend.disposed);
    }

    @Test
    void replacesAChangedTypeEvenWhenItsKeyIsUnchanged() {
        RecordingBackend backend = new RecordingBackend();
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(column(text("same", "Text")));
            var old = tree.root().children.getFirst();
            tree.render(column(ViewNode.button("Button").withKey("same")));
            assertNotSame(old, tree.root().children.getFirst());
            assertTrue(old.disposed);
            assertEquals("button", tree.root().children.getFirst().node.type());
        }
    }

    @Test
    void refreshesCallbacksOnUnchangedVisualProperties() {
        AtomicReference<String> result = new AtomicReference<>();
        RecordingBackend backend = new RecordingBackend();
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(button(result, "old"));
            var handle = tree.root();
            tree.render(button(result, "new"));
            assertSame(handle, tree.root());
            handle.node.dispatch("click");
            assertEquals("new", result.get());
        }
    }

    @Test
    void rejectsDuplicateKeysBeforeMutatingTheMountedTree() {
        RecordingBackend backend = new RecordingBackend();
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(column(text("a", "A")));
            var original = tree.root();
            assertThrows(IllegalArgumentException.class, () -> tree.render(column(text("a", "One"), text("a", "Two"))));
            assertSame(original, tree.root());
            assertEquals(2, backend.created);
            assertEquals("A", original.children.getFirst().node.text());
        }
    }

    @Test
    void rootReplacementAndRepeatedCloseDisposeExactlyOnce() {
        RecordingBackend backend = new RecordingBackend();
        MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend);
        tree.render(column(text("a", "A")));
        tree.render(ViewNode.button("New root"));
        assertEquals(2, backend.disposed);
        tree.close();
        tree.close();
        assertNull(tree.root());
        assertEquals(3, backend.disposed);
        assertThrows(IllegalStateException.class, () -> tree.render(ViewNode.text("Closed")));
    }

    @Test
    void failedRootAttachmentDisposesTheReplacementAndKeepsThePreviousRoot() {
        RecordingBackend backend = new RecordingBackend() {
            @Override public void setRoot(Handle root) {
                if (root != null && root.node.type().equals("button")) {
                    throw new IllegalArgumentException("Cannot attach root");
                }
                super.setRoot(root);
            }
        };
        try (MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend)) {
            tree.render(column(text("a", "A")));
            var root = tree.root();
            assertThrows(IllegalArgumentException.class, () -> tree.render(ViewNode.button("Rejected")));
            assertSame(root, tree.root());
            assertSame(root, backend.root);
            assertEquals(1, backend.disposed);
        }
        assertEquals(backend.created, backend.disposed);
    }

    @Test
    void cleanupContinuesAfterOneBackendDisposalFails() {
        RecordingBackend backend = new RecordingBackend() {
            @Override public void dispose(Handle handle) {
                super.dispose(handle);
                if ("a".equals(handle.node.key())) {
                    throw new IllegalArgumentException("Disposal failed");
                }
            }
        };
        MountedTree<RecordingBackend.Handle> tree = new MountedTree<>(backend);
        tree.render(column(text("a", "A"), text("b", "B")));
        assertThrows(IllegalArgumentException.class, tree::close);
        assertEquals(3, backend.disposed);
        assertNull(backend.root);
    }

    private ViewNode column(ViewNode... children) {
        var builder = ViewNode.builder("column");
        for (ViewNode child : children) {
            builder.child(child);
        }
        return builder.build();
    }

    private ViewNode text(String key, String value) {
        return ViewNode.text(value).withKey(key);
    }

    private ViewNode button(AtomicReference<String> result, String capturedValue) {
        return ViewNode.builder("button").prop("label", "Same")
                .on("click", event -> result.set(capturedValue)).build();
    }
}
