package dev.vela.tooling;

import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.SourceVersion;

public record ProjectConfig(String packageName, String entry, String name, int minSdk, int compileSdk, int targetSdk) {
    public ProjectConfig {
        if (!validPackage(packageName)) throw new IllegalArgumentException("Invalid package: " + packageName);
        if (!entry.startsWith(packageName + ".") || !validPackage(entry)) throw new IllegalArgumentException("Entry must be a class in " + packageName);
        if (name.isBlank()) throw new IllegalArgumentException("Application name must not be blank");
        if (minSdk < 26 || compileSdk < minSdk || targetSdk > compileSdk || targetSdk < minSdk)
            throw new IllegalArgumentException("Require 26 <= minSdk <= targetSdk <= compileSdk");
    }
    public static ProjectConfig read(Path project) throws IOException {
        YamlDocument yaml = YamlDocument.read(project.resolve("vela.yaml"));
        for (String key : yaml.values().keySet()) if (!java.util.Set.of("package", "entry", "name", "minSdk", "compileSdk", "targetSdk").contains(key))
            throw yaml.error(key, "Unknown application setting: " + key);
        String pkg = yaml.get("package", "");
        try {
            return new ProjectConfig(pkg, yaml.get("entry", pkg + ".MainScreen"), yaml.get("name", "Vela App"),
                    Integer.parseInt(yaml.get("minSdk", "26")), Integer.parseInt(yaml.get("compileSdk", "35")), Integer.parseInt(yaml.get("targetSdk", "35")));
        } catch (IllegalArgumentException failure) { throw yaml.error("package", failure.getMessage()); }
    }
    public static boolean validPackage(String value) {
        if (value == null || !value.contains(".")) return false;
        for (String part : value.split("\\.", -1)) if (!SourceVersion.isIdentifier(part) || SourceVersion.isKeyword(part)) return false;
        return true;
    }
}
