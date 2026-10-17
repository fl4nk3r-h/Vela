package dev.vela.resources;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Deterministic, portable resource data. No YAML parser or object deserialization at runtime. */
public final class ResourceCatalog {
    private static final int MAGIC = 0x56454c31;
    public static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Map<String, Map<String, String>> locales;

    public ResourceCatalog(Map<String, Map<String, String>> locales) {
        Map<String, Map<String, String>> copy = new TreeMap<>();
        locales.forEach((key, value) -> copy.put(key, Collections.unmodifiableMap(new TreeMap<>(value))));
        copy.putIfAbsent("", Map.of());
        this.locales = Collections.unmodifiableMap(copy);
    }

    public static ResourceCatalog empty() { return new ResourceCatalog(Map.of()); }
    public Map<String, Map<String, String>> locales() { return locales; }

    public Map<String, String> values(Locale locale) {
        Map<String, String> result = new LinkedHashMap<>(locales.get(""));
        String language = locale.getLanguage();
        result.putAll(locales.getOrDefault(language, Map.of()));
        String script = locale.getScript();
        if (!script.isEmpty()) result.putAll(locales.getOrDefault(language + "-" + script, Map.of()));
        result.putAll(locales.getOrDefault(locale.toLanguageTag(), Map.of()));
        return Collections.unmodifiableMap(result);
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC); out.writeInt(locales.size());
            for (var locale : locales.entrySet()) {
                write(out, locale.getKey()); out.writeInt(locale.getValue().size());
                for (var entry : locale.getValue().entrySet()) { write(out, entry.getKey()); write(out, entry.getValue()); }
            }
        }
        if (bytes.size() > MAX_BYTES) throw new IOException("Resource catalog exceeds 16 MiB");
        return bytes.toByteArray();
    }

    public static ResourceCatalog decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IOException("Resource catalog exceeds 16 MiB");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != MAGIC) throw new IOException("Unsupported Vela resource format");
            Map<String, Map<String, String>> locales = new TreeMap<>();
            int count = count(in, 1000);
            for (int i = 0; i < count; i++) {
                String locale = read(in); Map<String, String> values = new TreeMap<>();
                int entries = count(in, 100000);
                for (int j = 0; j < entries; j++) {
                    if (values.put(read(in), read(in)) != null) throw new IOException("Duplicate resource key");
                }
                if (locales.put(locale, values) != null) throw new IOException("Duplicate resource locale");
            }
            if (in.available() != 0) throw new IOException("Trailing resource data");
            return new ResourceCatalog(locales);
        }
    }

    private static int count(DataInputStream in, int max) throws IOException {
        int size = in.readInt();
        if (size < 0 || size > max) throw new IOException("Invalid resource count");
        return size;
    }
    private static void write(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
    }
    private static String read(DataInputStream in) throws IOException {
        int size = count(in, MAX_BYTES);
        if (size > in.available()) throw new EOFException("Truncated resource value");
        byte[] bytes = new byte[size]; in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
