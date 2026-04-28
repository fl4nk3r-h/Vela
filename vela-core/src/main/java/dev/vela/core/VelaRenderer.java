package dev.vela.core;

public final class VelaRenderer {
    public String render(ViewNode node) {
        StringBuilder builder = new StringBuilder();
        render(node, builder, 0);
        return builder.toString();
    }

    private void render(ViewNode node, StringBuilder builder, int depth) {
        builder.append("  ".repeat(Math.max(0, depth)))
            .append(node.type());

        if (node.text() != null) {
            builder.append("[").append(node.text()).append("]");
        }

        if (!node.props().isEmpty()) {
            builder.append(" ").append(node.props());
        }

        builder.append(System.lineSeparator());

        for (ViewNode child : node.children()) {
            render(child, builder, depth + 1);
        }
    }
}