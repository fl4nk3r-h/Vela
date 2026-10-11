package dev.vela.components;

import java.util.List;
import java.util.Objects;

import dev.vela.core.ViewNode;
import dev.vela.ui.Modifier;

public final class VelaTable {
    private VelaTable() {
    }

    public static ViewNode of(List<String> headers, List<List<String>> rows, Modifier modifier) {
        List<String> immutableHeaders = List.copyOf(Objects.requireNonNull(headers, "headers"));
        if (immutableHeaders.isEmpty()) {
            throw new IllegalArgumentException("headers must not be empty");
        }

        ViewNode.Builder table = ComponentSupport.node("table", modifier.role("table"))
                .prop("columnCount", immutableHeaders.size())
                .child(row(immutableHeaders, true, 0));
        List<List<String>> immutableRows = Objects.requireNonNull(rows, "rows").stream()
                .map(List::copyOf)
                .toList();
        for (int index = 0; index < immutableRows.size(); index++) {
            List<String> values = immutableRows.get(index);
            if (values.size() != immutableHeaders.size()) {
                throw new IllegalArgumentException("Every row must have the same number of cells as headers");
            }
            table.child(row(values, false, index));
        }
        return table.prop("rowCount", immutableRows.size()).build();
    }

    private static ViewNode row(List<String> values, boolean header, int index) {
        ViewNode.Builder row = ViewNode.builder("table-row")
                .prop("header", header)
                .prop("index", index);
        for (String value : values) {
            row.child(ViewNode.builder("table-cell")
                    .prop("value", Objects.requireNonNull(value, "cell value"))
                    .prop("header", header)
                    .build());
        }
        return row.build();
    }
}
