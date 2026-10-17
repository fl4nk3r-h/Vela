package dev.vela.dev;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Versioned, length-bounded development protocol. Included only by development tooling. */
public final class ReloadProtocol {
    public static final int MAX_DEX = 64 * 1024 * 1024;
    public static final int MAX_RESOURCES = 16 * 1024 * 1024;
    private static final int MAGIC = 0x56454c41;
    private ReloadProtocol() { }
    public enum Operation { PING, CODE, RESOURCES, CHECKPOINT }
    public record Request(Operation operation, String entry, byte[] dex, byte[] resources, List<String> classes) {
        public Request { classes = List.copyOf(classes); }
        public Request(Operation operation, String entry, byte[] dex, byte[] resources) { this(operation, entry, dex, resources, List.of()); }
    }
    public record Response(boolean success, String message) { }

    public static void writeRequest(DataOutputStream out, String token, Request request) throws IOException {
        out.writeInt(MAGIC); out.writeInt(2); out.writeUTF(token); out.writeUTF(request.operation().name());
        out.writeUTF(request.entry());
        if (request.classes().size() > 20000) throw new IOException("Too many reload classes");
        out.writeInt(request.classes().size()); int budget = 2 * 1024 * 1024;
        for (String name : request.classes()) {
            budget -= name.length();
            if (name.length() > 512 || budget < 0) throw new IOException("Reload class manifest is too large");
            out.writeUTF(name);
        }
        writeBytes(out, request.dex(), MAX_DEX); writeBytes(out, request.resources(), MAX_RESOURCES); out.flush();
    }
    public static Request readRequest(DataInputStream in, String token) throws IOException {
        if (in.readInt() != MAGIC || in.readInt() != 2) throw new IOException("Incompatible Vela development protocol");
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), in.readUTF().getBytes(StandardCharsets.UTF_8))) throw new IOException("Development session authentication failed");
        Operation operation;
        try { operation = Operation.valueOf(in.readUTF()); }
        catch (IllegalArgumentException error) { throw new IOException("Unknown development operation"); }
        String entry = in.readUTF(); int count = in.readInt();
        if (count < 0 || count > 20000) throw new IOException("Invalid reload class count");
        List<String> classes = new ArrayList<>(); int budget = 2 * 1024 * 1024;
        for (int i = 0; i < count; i++) {
            String name = in.readUTF(); budget -= name.length();
            if (name.length() > 512 || budget < 0) throw new IOException("Reload class manifest is too large");
            classes.add(name);
        }
        return new Request(operation, entry, readBytes(in, MAX_DEX), readBytes(in, MAX_RESOURCES), classes);
    }
    public static void writeResponse(DataOutputStream out, Response response) throws IOException {
        out.writeBoolean(response.success());
        String message = response.message();
        out.writeUTF(message.length() > 8000 ? message.substring(0, 8000) : message); out.flush();
    }
    public static Response readResponse(DataInputStream in) throws IOException { return new Response(in.readBoolean(), in.readUTF()); }
    private static void writeBytes(DataOutputStream out, byte[] value, int limit) throws IOException {
        if (value.length > limit) throw new IOException("Development payload is too large");
        out.writeInt(value.length); out.write(value);
    }
    private static byte[] readBytes(DataInputStream in, int limit) throws IOException {
        int size = in.readInt();
        if (size < 0 || size > limit) throw new IOException("Invalid development payload size");
        byte[] bytes = new byte[size]; in.readFully(bytes); return bytes;
    }
}
