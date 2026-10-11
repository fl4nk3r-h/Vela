package dev.vela.core;

import java.util.List;

public final class TextRendererBackend implements RendererBackend<TextRendererBackend.Node> {
    private final VelaRenderer renderer = new VelaRenderer();
    private Node root;

    @Override
    public Node create(ViewNode node) {
        return new Node(node);
    }

    @Override
    public void update(Node handle, ViewNode previous, ViewNode next) {
        handle.description = next;
    }

    @Override
    public void setChildren(Node parent, List<Node> children) {
        parent.children = List.copyOf(children);
    }

    @Override
    public void setRoot(Node root) {
        this.root = root;
    }

    @Override
    public void dispose(Node handle) {
        handle.children = List.of();
        handle.description = null;
    }

    public String output() {
        return root == null ? "" : renderer.render(root.description);
    }

    public static final class Node {
        private ViewNode description;
        private List<Node> children = List.of();

        private Node(ViewNode description) {
            this.description = description;
        }

        public ViewNode description() {
            if (description == null) {
                throw new IllegalStateException("Text node is disposed");
            }
            return description;
        }

        public List<Node> children() {
            return children;
        }

        public boolean dispatch(String event) {
            return description != null && description.dispatch(event);
        }

        public boolean dispatch(String event, Object value) {
            return description != null && description.dispatch(event, value);
        }
    }
}
