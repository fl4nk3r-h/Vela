package dev.vela.tooling;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.*;

/** Restricted data-only YAML with source locations retained for diagnostics. */
public final class YamlDocument {
    private final Path file;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, Node> locations = new HashMap<>();
    private YamlDocument(Path file) { this.file = file; }

    public static YamlDocument read(Path file) throws IOException {
        YamlDocument document = new YamlDocument(file);
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false); options.setMaxAliasesForCollections(0);
        options.setCodePointLimit(2 * 1024 * 1024); options.setNestingDepthLimit(20);
        try (Reader reader = Files.newBufferedReader(file)) {
            Node root = new Yaml(new SafeConstructor(options)).compose(reader);
            if (root != null) document.flatten("", root);
            return document;
        } catch (MarkedYAMLException error) {
            int line = error.getProblemMark() == null ? 1 : error.getProblemMark().getLine() + 1;
            throw new IllegalArgumentException(file + ":" + line + ": " + error.getProblem(), error);
        }
    }

    private void flatten(String prefix, Node node) {
        if (node instanceof MappingNode mapping && Tag.MAP.equals(node.getTag())) {
            Set<String> seen = new HashSet<>();
            for (NodeTuple entry : mapping.getValue()) {
                if (!(entry.getKeyNode() instanceof ScalarNode key) || !Tag.STR.equals(key.getTag()))
                    throw error(node, "Keys must be strings");
                if (!key.getValue().matches("[A-Za-z_][A-Za-z0-9_.-]*")) throw error(key, "Invalid key: " + key.getValue());
                if (!seen.add(key.getValue())) throw error(key, "Duplicate key: " + key.getValue());
                String name = prefix.isEmpty() ? key.getValue() : prefix + "." + key.getValue();
                locations.put(name, entry.getValueNode());
                flatten(name, entry.getValueNode());
            }
        } else if (node instanceof ScalarNode scalar && Set.of(Tag.STR, Tag.INT, Tag.FLOAT, Tag.BOOL).contains(node.getTag())) {
            if (values.put(prefix, scalar.getValue()) != null) throw error(node, "Duplicate resource: " + prefix);
        } else throw error(node, "Use mappings and scalar values only; quote colors and strings beginning with #");
    }

    public Map<String, String> values() { return Collections.unmodifiableMap(values); }
    public String get(String key, String fallback) { return values.getOrDefault(key, fallback); }
    public IllegalArgumentException error(String key, String message) {
        return error(locations.get(key), message);
    }
    private IllegalArgumentException error(Node node, String message) {
        int line = node == null ? 1 : node.getStartMark().getLine() + 1;
        int column = node == null ? 1 : node.getStartMark().getColumn() + 1;
        return new IllegalArgumentException(file + ":" + line + ":" + column + ": " + message);
    }
}
