package dev.vela.cli;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.HexFormat;

/** Reusable offline cache; interrupted or unverified files never become cache hits. */
public final class VerifiedDownload {
    public Path download(URI uri, String sha256, Path target) throws IOException, InterruptedException {
        if (!sha256.matches("[0-9a-fA-F]{64}")) throw new IOException("Missing SHA-256 download checksum");
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (FileChannel channel = FileChannel.open(target.resolveSibling(target.getFileName() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            if (Files.isRegularFile(target) && digest(target).equalsIgnoreCase(sha256)) return target;
            Files.deleteIfExists(target);
            Path partial = target.resolveSibling(target.getFileName() + ".part");
            try {
                InputStream body;
                if (uri.getScheme().equals("file")) body = Files.newInputStream(Path.of(uri));
                else {
                    if (!uri.getScheme().equals("https") && !(uri.getScheme().equals("http") && InetAddress.getByName(uri.getHost()).isLoopbackAddress()))
                        throw new IOException("Downloads require HTTPS");
                    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build();
                    HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(15)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() != 200) { response.body().close(); throw new IOException("Download returned HTTP " + response.statusCode()); }
                    body = response.body();
                }
                try (InputStream in = body; OutputStream out = Files.newOutputStream(partial)) {
                    byte[] buffer = new byte[65536]; long total = 0; int count;
                    while ((count = in.read(buffer)) != -1) {
                        total += count; if (total > 3L * 1024 * 1024 * 1024) throw new IOException("Download exceeds size limit");
                        out.write(buffer, 0, count);
                    }
                }
                if (!digest(partial).equalsIgnoreCase(sha256)) throw new IOException("Download checksum mismatch: " + uri.getHost());
                try { Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException error) { Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING); }
                return target;
            } finally { Files.deleteIfExists(partial); }
        }
    }
    public static String digest(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[65536]; int count;
            while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
