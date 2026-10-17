package dev.vela.cli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class Programs {
    private Programs() { }
    static List<String> command(Path executable, String... arguments) {
        List<String> result = new ArrayList<>();
        if (executable.toString().endsWith(".bat")) result.addAll(List.of("cmd", "/c"));
        result.add(executable.toString()); result.addAll(List.of(arguments)); return result;
    }
    static int run(List<String> command, Path directory, Map<String, String> environment) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).inheritIO();
        builder.environment().putAll(environment); return await(builder.start());
    }
    static String capture(List<String> command, Path directory, Map<String, String> environment) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
        builder.environment().putAll(environment); Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) { in.transferTo(output); }
        int status = await(process); String text = output.toString(StandardCharsets.UTF_8);
        if (status != 0) throw new IOException("Command failed (" + status + "): " + String.join(" ", command) + "\n" + text);
        return text.strip();
    }
    private static int await(Process process) throws InterruptedException, IOException {
        try {
            if (!process.waitFor(20, TimeUnit.MINUTES)) { process.destroyForcibly(); throw new IOException("Command timed out"); }
            return process.exitValue();
        } catch (InterruptedException interrupted) { process.destroyForcibly(); Thread.currentThread().interrupt(); throw interrupted; }
    }
}
