package dev.vela.tooling;

import dev.vela.resources.ResourceCatalog;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.*;
import javax.lang.model.SourceVersion;

/** Incremental YAML compilation shared by Gradle and the development CLI. */
public final class ResourceCompiler {
    private static final Set<String> GROUPS = Set.of("strings", "colors", "dimensions", "styles", "themes");
    private static final Pattern PARAMETER = Pattern.compile("\\{(\\d+)(?:,([^,}]+)[^}]*)?}");
    private static final Map<String, String> PROPERTIES = Map.ofEntries(
        Map.entry("background", "style.background"), Map.entry("foreground", "style.foreground"),
        Map.entry("padding", "style.padding"), Map.entry("margin", "style.margin"),
        Map.entry("width", "style.width"), Map.entry("height", "style.height"),
        Map.entry("cornerRadius", "style.cornerRadius"), Map.entry("shadow", "style.shadow"),
        Map.entry("color", "text.color"), Map.entry("fontSize", "text.fontSize"),
        Map.entry("fontWeight", "text.fontWeight"), Map.entry("fontFamily", "text.fontFamily"),
        Map.entry("gap", "layout.gap"), Map.entry("opacity", "style.opacity"),
        Map.entry("lineHeight", "text.lineHeight"), Map.entry("maxLines", "text.maxLines"));

    public record Result(ResourceCatalog catalog, String source) { }

    public Result compile(Path directory, String packageName) throws IOException {
        if (!ProjectConfig.validPackage(packageName)) throw new IllegalArgumentException("Invalid resource package");
        Path base = directory.resolve("values.yaml");
        YamlDocument defaults = Files.exists(base) ? YamlDocument.read(base) : null;
        Map<String, String> baseValues = defaults == null ? Map.of() : resolve(defaults, Map.of());
        Map<String, Map<String, String>> locales = new TreeMap<>(); locales.put("", baseValues);
        if (Files.isDirectory(directory)) try (var files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().matches("values-[A-Za-z0-9-]+\\.yaml")).sorted().toList()) {
                String tag = file.getFileName().toString().replaceFirst("^values-", "").replaceFirst("\\.yaml$", "");
                Locale locale;
                try { locale = new Locale.Builder().setLanguageTag(tag).build(); }
                catch (IllformedLocaleException failure) { throw new IllegalArgumentException(file + ":1: Invalid locale " + tag); }
                YamlDocument translation = YamlDocument.read(file);
                for (String key : translation.values().keySet()) {
                    if (!key.startsWith("strings.") || !baseValues.containsKey(key)) throw translation.error(key, "Translations must reference a default string");
                }
                Map<String, String> translated = resolve(translation, baseValues);
                for (String key : translation.values().keySet()) {
                    if (!parameters(baseValues.get(key)).equals(parameters(translated.get(key))))
                        throw translation.error(key, "Translation parameters differ from the default string");
                }
                if (locales.put(locale.toLanguageTag(), translated) != null) throw translation.error("", "Duplicate locale " + tag);
            }
        }
        ResourceCatalog catalog = new ResourceCatalog(locales);
        return new Result(catalog, source(packageName, baseValues));
    }

    public void generate(Path directory, String packageName, Path output) throws IOException {
        Result result = compile(directory, packageName);
        writeIfChanged(output.resolve("java/" + packageName.replace('.', '/') + "/Values.java"), result.source().getBytes(StandardCharsets.UTF_8));
        writeIfChanged(output.resolve("assets/vela/values.bin"), result.catalog().encode());
        Set<Path> expected = new HashSet<>();
        for (var locale : result.catalog().locales().entrySet()) {
            String qualifier = locale.getKey().isEmpty() ? "values" : "values-b+" + locale.getKey().replace('-', '+');
            Path file = output.resolve("res/" + qualifier + "/vela_values.xml"); expected.add(file.toAbsolutePath());
            StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
            for (var entry : locale.getValue().entrySet()) {
                String key = entry.getKey(); String type = key.startsWith("strings.") ? "string" : key.startsWith("colors.") ? "color" : key.startsWith("dimensions.") ? "dimen" : null;
                if (type == null) continue;
                String name = "vela_" + key.substring(key.indexOf('.') + 1);
                String value = xml(entry.getValue());
                if (type.equals("string")) value = value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n");
                xml.append("  <").append(type).append(" name=\"").append(name).append("\"");
                if (type.equals("string")) xml.append(" formatted=\"false\"");
                xml.append(">").append(value).append("</").append(type).append(">\n");
            }
            xml.append("</resources>\n"); writeIfChanged(file, xml.toString().getBytes(StandardCharsets.UTF_8));
        }
        Path res = output.resolve("res");
        if (Files.isDirectory(res)) try (var files = Files.walk(res)) {
            for (Path file : files.filter(p -> p.getFileName().toString().equals("vela_values.xml")).toList())
                if (!expected.contains(file.toAbsolutePath())) Files.delete(file);
        }
    }

    private Map<String, String> resolve(YamlDocument yaml, Map<String, String> fallback) {
        Map<String, String> all = new TreeMap<>(fallback); all.putAll(yaml.values());
        Map<String, String> memo = new HashMap<>();
        Map<String, String> resolved = new TreeMap<>();
        for (String key : yaml.values().keySet()) {
            String[] parts = key.split("\\.");
            if (!GROUPS.contains(parts[0]) || parts.length < 2) throw yaml.error(key, "Unknown resource group");
            if (!parts[1].matches("[a-z][a-z0-9_]*") || Set.of("wait", "notify").contains(parts[1]))
                throw yaml.error(key, "Resource names must be lowercase Java identifiers");
            boolean style = parts[0].equals("styles") || parts[0].equals("themes");
            if (!style && parts.length != 2) throw yaml.error(key, "Resource values must be scalars");
            if (style && parts.length < 3) throw yaml.error(key, "Styles and themes must contain properties");
            String value = reference(key, all, new HashSet<>(), memo, yaml);
            if (parts[0].equals("colors") && !value.matches("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")) throw yaml.error(key, "Use a quoted #RRGGBB or #AARRGGBB color");
            if (parts[0].equals("dimensions") && !value.matches("[0-9]+(?:dp|sp)")) throw yaml.error(key, "Dimensions must be nonnegative integer dp or sp values");
            if (parts[0].equals("dimensions")) try { Integer.parseInt(value.substring(0, value.length() - 2)); }
                catch (NumberFormatException error) { throw yaml.error(key, "Dimension is too large"); }
            if (parts[0].equals("strings")) try { new MessageFormat(value, Locale.ROOT); parameters(value); }
                catch (IllegalArgumentException error) { throw yaml.error(key, "Invalid string format: " + error.getMessage()); }
            if (style) {
                String property = key.substring(parts[0].length() + parts[1].length() + 2);
                String wire = PROPERTIES.get(property);
                if (wire == null && PROPERTIES.containsValue(property)) wire = property;
                if (wire == null) throw yaml.error(key, "Unknown style property: " + property);
                validateProperty(wire, value, key, yaml);
                key = parts[0] + "." + parts[1] + "." + wire;
            }
            if (resolved.put(key, value) != null) throw yaml.error(key, "Duplicate style property");
        }
        return resolved;
    }

    private static void validateProperty(String prop, String value, String key, YamlDocument yaml) {
        if (Set.of("style.background", "style.foreground", "text.color").contains(prop) && !value.matches("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")) throw yaml.error(key, "Invalid color");
        if (Set.of("style.padding", "style.margin", "style.width", "style.height", "style.cornerRadius", "style.shadow", "layout.gap").contains(prop) && !value.matches("[0-9]+dp")) throw yaml.error(key, "Expected integer dp dimension");
        if (prop.equals("text.fontSize") && !value.matches("[1-9][0-9]*sp")) throw yaml.error(key, "Expected positive integer sp dimension");
        if (value.matches("[0-9]+(?:dp|sp)")) try { Integer.parseInt(value.substring(0, value.length() - 2)); }
            catch (NumberFormatException error) { throw yaml.error(key, "Dimension is too large"); }
        if (prop.equals("text.fontWeight") && !value.matches("[1-9]00")) throw yaml.error(key, "Font weight must be 100 through 900");
        if (Set.of("style.opacity", "text.lineHeight", "text.maxLines").contains(prop)) {
            try {
                double number = Double.parseDouble(value);
                if (!Double.isFinite(number) || number < 0 || (prop.equals("style.opacity") && number > 1)
                        || (!prop.equals("style.opacity") && number <= 0)) throw new NumberFormatException();
                if (prop.equals("text.maxLines")) Integer.parseInt(value);
            } catch (NumberFormatException error) { throw yaml.error(key, "Invalid numeric style value"); }
        }
    }

    private String reference(String key, Map<String, String> values, Set<String> visiting, Map<String, String> memo, YamlDocument yaml) {
        if (memo.containsKey(key)) return memo.get(key);
        if (visiting.size() >= 256) throw yaml.error(key, "Resource reference chain exceeds 256 entries");
        if (!visiting.add(key)) throw yaml.error(key, "Resource reference cycle: " + key);
        String value = values.get(key);
        if (value == null) throw yaml.error(key, "Missing resource reference: " + key);
        if (value.startsWith("@")) value = reference(value.substring(1), values, visiting, memo, yaml);
        visiting.remove(key); memo.put(key, value); return value;
    }

    private static Map<Integer, String> parameters(String value) {
        StringBuilder active = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'') {
                if (i + 1 < value.length() && value.charAt(i + 1) == '\'') { i++; continue; }
                quoted = !quoted;
            } else if (!quoted) active.append(current);
        }
        Map<Integer, String> result = new TreeMap<>(); Matcher matcher = PARAMETER.matcher(active);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index > 31) throw new IllegalArgumentException("At most 32 string arguments are supported");
            String type = Objects.requireNonNullElse(matcher.group(2), "object").strip();
            String previous = result.put(index, type);
            if (previous != null && !previous.equals(type)) throw new IllegalArgumentException("Argument has inconsistent types");
        }
        if (!result.isEmpty() && result.size() != result.keySet().stream().max(Integer::compareTo).orElse(-1) + 1)
            throw new IllegalArgumentException("String arguments must be consecutive from {0}");
        return result;
    }

    private static String source(String pkg, Map<String, String> values) {
        StringBuilder out = new StringBuilder("package " + pkg + ";\n\nimport dev.vela.resources.ResourceResolver;\nimport dev.vela.ui.Modifier;\n\n/** Generated resource API; values remain in the resource catalog. */\npublic final class Values {\n  private final ResourceResolver r;\n  private Values(ResourceResolver r) { this.r = r; }\n  public static Values of(ResourceResolver r) { return new Values(r); }\n");
        for (String group : List.of("strings", "colors", "dimensions", "styles", "themes")) {
            String cls = Character.toUpperCase(group.charAt(0)) + group.substring(1);
            out.append("  public ").append(cls).append(' ').append(group).append("() { return new ").append(cls).append("(); }\n  public final class ").append(cls).append(" {\n");
            Set<String> names = new TreeSet<>();
            for (String key : values.keySet()) if (key.startsWith(group + ".")) names.add(key.split("\\.")[1]);
            for (String name : names) {
                String method = SourceVersion.isKeyword(name) ? name + "Value" : name;
                String type = group.equals("dimensions") ? "int" : Set.of("styles", "themes").contains(group) ? "Modifier" : "String";
                out.append("    public ").append(type).append(' ').append(method).append('(');
                Map<Integer, String> params = group.equals("strings") ? parameters(values.get(group + "." + name)) : Map.of();
                for (int i = 0; i < params.size(); i++) {
                    if (i > 0) out.append(", ");
                    String argType = Set.of("number", "choice").contains(params.get(i)) ? "Number" : Set.of("date", "time").contains(params.get(i)) ? "java.util.Date" : "Object";
                    out.append(argType).append(" arg").append(i);
                }
                out.append(") { return ");
                out.append("r.").append(group.equals("strings") ? "string" : group.equals("colors") ? "color" : group.equals("dimensions") ? "dimension" : group.equals("themes") ? "theme" : "style").append("(\"").append(name).append('"');
                for (int i = 0; i < params.size(); i++) out.append(", arg").append(i);
                out.append(')');
                out.append("; }\n");
            }
            out.append("  }\n");
        }
        return out.append("}\n").toString();
    }

    public static void writeIfChanged(Path file, byte[] bytes) throws IOException {
        if (Files.exists(file) && Arrays.equals(Files.readAllBytes(file), bytes)) return;
        Files.createDirectories(file.toAbsolutePath().getParent()); Files.write(file, bytes);
    }
    public static String xml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
