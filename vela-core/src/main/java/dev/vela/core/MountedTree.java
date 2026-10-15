package dev.vela.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

public final class MountedTree<H> implements AutoCloseable {
    private final RendererBackend<H> backend;
    private final Thread owner = Thread.currentThread();
    private MountedNode<H> root;
    private boolean closed;
    private final Map<ViewNode, Boolean> validated = new WeakHashMap<>();

    public MountedTree(RendererBackend<H> backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public H render(ViewNode node) {
        checkOpen();
        validateKeys(Objects.requireNonNull(node, "node"));
        if (root != null && reusable(root, node)) {
            patch(root, node);
        } else {
            MountedNode<H> replacement = mount(node);
            try {
                backend.setRoot(replacement.handle);
            } catch (RuntimeException | Error failure) {
                try {
                    dispose(replacement);
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
            MountedNode<H> previous = root;
            root = replacement;
            if (previous != null) {
                dispose(previous);
            }
        }
        return root.handle;
    }

    public H root() {
        checkThread();
        return root == null ? null : root.handle;
    }

    private MountedNode<H> mount(ViewNode node) {
        MountedNode<H> mounted = new MountedNode<>(node, Objects.requireNonNull(backend.create(node), "backend handle"));
        try {
            for (ViewNode child : node.children()) {
                mounted.children.add(mount(child));
            }
            if (!mounted.children.isEmpty()) {
                backend.setChildren(mounted.handle, handles(mounted.children));
            }
            return mounted;
        } catch (RuntimeException | Error failure) {
            try {
                dispose(mounted);
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private void patch(MountedNode<H> mounted, ViewNode next) {
        if (mounted.node == next) {
            return;
        }
        // Always update the description so a retained view receives current callbacks.
        backend.update(mounted.handle, mounted.node, next);
        List<MountedNode<H>> previous = mounted.children;
        Map<String, MountedNode<H>> keyed = new HashMap<>();
        for (MountedNode<H> child : previous) {
            if (child.node.key() != null) {
                keyed.put(child.node.key(), child);
            }
        }

        List<MountedNode<H>> children = new ArrayList<>(next.children().size());
        Set<MountedNode<H>> retained = new HashSet<>();
        List<MountedNode<H>> created = new ArrayList<>();
        try {
            for (int index = 0; index < next.children().size(); index++) {
                ViewNode child = next.children().get(index);
                MountedNode<H> candidate = child.key() == null
                        ? (index < previous.size() ? previous.get(index) : null)
                        : keyed.get(child.key());
                if (candidate != null && reusable(candidate, child)) {
                    patch(candidate, child);
                    children.add(candidate);
                    retained.add(candidate);
                } else {
                    MountedNode<H> fresh = mount(child);
                    created.add(fresh);
                    children.add(fresh);
                }
            }
            if (!sameOrder(previous, children)) {
                backend.setChildren(mounted.handle, handles(children));
            }
        } catch (RuntimeException | Error failure) {
            for (MountedNode<H> fresh : created) {
                try {
                    dispose(fresh);
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
        mounted.node = next;
        mounted.children = children;
        Throwable cleanupFailure = null;
        for (MountedNode<H> child : previous) {
            if (!retained.contains(child)) {
                try {
                    dispose(child);
                } catch (RuntimeException | Error failure) {
                    cleanupFailure = accumulate(cleanupFailure, failure);
                }
            }
        }
        rethrow(cleanupFailure);
    }

    private boolean reusable(MountedNode<H> mounted, ViewNode node) {
        return mounted.node.type().equals(node.type()) && Objects.equals(mounted.node.key(), node.key());
    }

    private boolean sameOrder(List<MountedNode<H>> previous, List<MountedNode<H>> next) {
        if (previous.size() != next.size()) {
            return false;
        }
        for (int index = 0; index < previous.size(); index++) {
            if (previous.get(index) != next.get(index)) {
                return false;
            }
        }
        return true;
    }

    private List<H> handles(List<MountedNode<H>> nodes) {
        List<H> handles = new ArrayList<>(nodes.size());
        for (MountedNode<H> node : nodes) {
            handles.add(node.handle);
        }
        return List.copyOf(handles);
    }

    private void validateKeys(ViewNode node) {
        if (validated.containsKey(node)) return;
        Set<String> keys = new HashSet<>();
        for (ViewNode child : node.children()) {
            if (child.key() != null && !keys.add(child.key())) {
                throw new IllegalArgumentException("Duplicate sibling key: " + child.key());
            }
            validateKeys(child);
        }
        validated.put(node, Boolean.TRUE);
    }

    private void dispose(MountedNode<H> node) {
        Throwable failure = null;
        if (!node.children.isEmpty()) {
            try {
                backend.setChildren(node.handle, List.of());
            } catch (RuntimeException | Error error) {
                failure = error;
            }
            for (MountedNode<H> child : node.children) {
                try {
                    dispose(child);
                } catch (RuntimeException | Error error) {
                    failure = accumulate(failure, error);
                }
            }
        }
        try {
            backend.dispose(node.handle);
        } catch (RuntimeException | Error error) {
            failure = accumulate(failure, error);
        }
        rethrow(failure);
    }

    private Throwable accumulate(Throwable failure, Throwable next) {
        if (failure == null) {
            return next;
        }
        failure.addSuppressed(next);
        return failure;
    }

    private void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException error) {
            throw error;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private void checkThread() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("Mounted tree must be accessed on its owning thread");
        }
    }

    private void checkOpen() {
        checkThread();
        if (closed) {
            throw new IllegalStateException("Mounted tree is closed");
        }
    }

    @Override
    public void close() {
        checkThread();
        if (closed) {
            return;
        }
        closed = true;
        MountedNode<H> previous = root;
        root = null;
        Throwable failure = null;
        try {
            backend.setRoot(null);
        } catch (RuntimeException | Error error) {
            failure = error;
        }
        if (previous != null) {
            try {
                dispose(previous);
            } catch (RuntimeException | Error error) {
                failure = accumulate(failure, error);
            }
        }
        rethrow(failure);
    }

    private static final class MountedNode<H> {
        private ViewNode node;
        private final H handle;
        private List<MountedNode<H>> children = new ArrayList<>();

        private MountedNode(ViewNode node, H handle) {
            this.node = node;
            this.handle = handle;
        }
    }
}
