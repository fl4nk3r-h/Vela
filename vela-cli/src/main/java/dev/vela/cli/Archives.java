package dev.vela.cli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.zip.*;

/** Archive extraction restricted to the requested directory. */
public final class Archives {
    private Archives() { }
    public static void extract(Path archive, Path directory) throws IOException {
        Files.createDirectories(directory);
        if (archive.toString().endsWith(".zip")) zip(archive, directory);
        else tar(archive, directory);
    }
    private static Path destination(Path directory, String name) throws IOException {
        Path target = directory.toAbsolutePath().resolve(name).normalize();
        if (!target.startsWith(directory.toAbsolutePath().normalize())) throw new IOException("Archive entry escapes installation directory");
        Path parent = target.getParent();
        while (parent != null && parent.startsWith(directory.toAbsolutePath())) {
            if (Files.isSymbolicLink(parent)) throw new IOException("Archive entry traverses a symbolic link"); parent = parent.getParent();
        }
        return target;
    }
    private static void zip(Path archive, Path directory) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path target = destination(directory, entry.getName());
                if (entry.isDirectory()) Files.createDirectories(target);
                else { Files.createDirectories(target.getParent()); Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING); executable(target, entry.getName().contains("/bin/")); }
            }
        }
    }
    private static void tar(Path archive, Path directory) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(archive)))) {
            byte[] header = new byte[512];
            while (true) {
                int first = in.read(); if (first < 0 || first == 0) break;
                header[0] = (byte) first; in.readFully(header, 1, 511);
                String name = text(header, 0, 100), prefix = text(header, 345, 155);
                if (!prefix.isEmpty()) name = prefix + "/" + name;
                long size;
                try { size = Long.parseLong(text(header, 124, 12).trim(), 8); }
                catch (NumberFormatException error) { throw new IOException("Invalid tar entry size", error); }
                if (size < 0 || size > 3L * 1024 * 1024 * 1024) throw new IOException("Invalid tar entry size");
                Path target = destination(directory, name); int type = header[156];
                if (type == '5') Files.createDirectories(target);
                else if (type == '2') {
                    String link = text(header, 157, 100);
                    Path resolved = target.getParent().resolve(link).normalize();
                    if (!resolved.startsWith(directory.toAbsolutePath().normalize())) throw new IOException("Archive symbolic link escapes installation directory");
                    Files.createDirectories(target.getParent()); Files.deleteIfExists(target); Files.createSymbolicLink(target, Path.of(link));
                } else if (type == 0 || type == '0') {
                    Files.createDirectories(target.getParent());
                    try (OutputStream out = Files.newOutputStream(target)) { copy(in, out, size); }
                    boolean execute = (Integer.parseInt(text(header, 100, 8).trim(), 8) & 0111) != 0;
                    executable(target, execute); size = -size; // content has already been consumed
                } else if (type != 'x' && type != 'g') throw new IOException("Unsupported tar entry type: " + (char) type);
                if (size > 0) in.skipNBytes(size);
                long original = Math.abs(size); in.skipNBytes((512 - original % 512) % 512);
            }
        }
    }
    private static void copy(InputStream in, OutputStream out, long size) throws IOException {
        byte[] bytes = new byte[65536]; long remaining = size;
        while (remaining > 0) { int count = in.read(bytes, 0, (int) Math.min(bytes.length, remaining)); if (count < 0) throw new EOFException("Truncated archive"); out.write(bytes, 0, count); remaining -= count; }
    }
    private static String text(byte[] bytes, int offset, int length) {
        int end = offset; while (end < offset + length && bytes[end] != 0) end++;
        return new String(bytes, offset, end - offset, StandardCharsets.UTF_8);
    }
    private static void executable(Path path, boolean executable) throws IOException {
        if (executable && Files.getFileStore(path).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
