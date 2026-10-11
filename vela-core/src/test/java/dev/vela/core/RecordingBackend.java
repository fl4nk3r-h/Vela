package dev.vela.core;

import java.util.ArrayList;
import java.util.List;

class RecordingBackend implements RendererBackend<RecordingBackend.Handle> {
    int created;
    int disposed;
    int childUpdates;
    Handle root;

    @Override
    public Handle create(ViewNode node) {
        created++;
        return new Handle(node);
    }

    @Override
    public void update(Handle handle, ViewNode previous, ViewNode next) {
        handle.node = next;
    }

    @Override
    public void setChildren(Handle parent, List<Handle> children) {
        childUpdates++;
        for (Handle old : parent.children) {
            if (!children.contains(old)) {
                old.parent = null;
            }
        }
        for (Handle child : children) {
            if (child.parent != null && child.parent != parent) {
                throw new AssertionError("Child must be detached before reparenting");
            }
            child.parent = parent;
        }
        parent.children = new ArrayList<>(children);
    }

    @Override
    public void setRoot(Handle root) {
        this.root = root;
    }

    @Override
    public void dispose(Handle handle) {
        if (handle.parent != null || !handle.children.isEmpty() || handle == root) {
            throw new AssertionError("Handle must be detached before disposal");
        }
        if (handle.disposed) {
            throw new AssertionError("Handle was disposed twice");
        }
        handle.disposed = true;
        disposed++;
    }

    static final class Handle {
        ViewNode node;
        List<Handle> children = new ArrayList<>();
        Handle parent;
        int selectionOffset;
        boolean disposed;

        Handle(ViewNode node) {
            this.node = node;
        }
    }
}
