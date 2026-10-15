package dev.vela.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VelaRenderer {
    public String render(ViewNode node) {
        StringBuilder builder = new StringBuilder();
        render(node, builder, 0);
        return builder.toString();
    }

    private void render(ViewNode node, StringBuilder builder, int depth) {
        boolean sensitive = "password".equals(node.props().get("inputType"))
                || Boolean.parseBoolean(node.props().get("semantics.sensitive"));
        builder.append("  ".repeat(Math.max(0, depth)))
            .append(node.type());

        if (node.key() != null) {
            builder.append("#").append(node.key());
        }

        if (node.text() != null) {
            builder.append("[").append(sensitive ? "<redacted>" : node.text()).append("]");
        }

        if (!node.props().isEmpty()) {
            Map<String, String> diagnosticProps = node.props();
            if (sensitive) {
                diagnosticProps = new LinkedHashMap<>();
                for (String name : node.props().keySet()) {
                    diagnosticProps.put(name, "<redacted>");
                }
            }
            builder.append(" ").append(diagnosticProps);
        }

        if (!node.eventNames().isEmpty()) {
            builder.append(" events=").append(node.eventNames());
        }

        builder.append(System.lineSeparator());

        for (ViewNode child : node.children()) {
            render(child, builder, depth + 1);
        }
    }
}
