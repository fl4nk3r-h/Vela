package dev.vela.cli;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstallerTest {
    @TempDir Path directory;
    @Test void verifiedDownloadsRemainUsableOfflineAndCorruptionIsDetected() throws Exception {
        Path source = directory.resolve("source"), cache = directory.resolve("cache/archive.zip"); Files.writeString(source, "verified payload");
        String checksum = VerifiedDownload.digest(source); var downloader = new VerifiedDownload();
        downloader.download(source.toUri(), checksum, cache); Files.delete(source);
        assertEquals(cache, downloader.download(source.toUri(), checksum, cache));
        Files.writeString(cache, "corrupt");
        assertThrows(IOException.class, () -> downloader.download(source.toUri(), checksum, cache));
        assertFalse(Files.exists(cache)); assertFalse(Files.exists(cache.resolveSibling("archive.zip.part")));
    }
    @Test void checksumMismatchAndInterruptedResponsesDoNotPoisonCache() throws Exception {
        Path source = directory.resolve("source"), cache = directory.resolve("cache.zip"); Files.writeString(source, "payload");
        assertThrows(IOException.class, () -> new VerifiedDownload().download(source.toUri(), "0".repeat(64), cache)); assertFalse(Files.exists(cache));
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/truncated", exchange -> { exchange.sendResponseHeaders(200, 100); exchange.getResponseBody().write(new byte[]{1, 2}); exchange.close(); });
        server.start();
        try {
            URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + "/truncated");
            assertThrows(IOException.class, () -> new VerifiedDownload().download(uri, "0".repeat(64), cache));
            assertFalse(Files.exists(cache)); assertFalse(Files.exists(directory.resolve("cache.zip.part")));
        } finally { server.stop(0); }
    }
    @Test void installerExtractsAtomicallyAndReusesCompletedInstallationOffline() throws Exception {
        Path archive = directory.resolve("jdk.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("jdk/bin/javac")); zip.write("compiler".getBytes()); zip.closeEntry();
        }
        String prefix = "jdk." + ToolchainInstaller.operatingSystem() + "." + ToolchainInstaller.architecture();
        Properties manifest = new Properties(); manifest.setProperty(prefix + ".url", archive.toUri().toString()); manifest.setProperty(prefix + ".sha256", VerifiedDownload.digest(archive));
        ToolchainInstaller installer = new ToolchainInstaller(directory.resolve("cache"), manifest, Map.of());
        Path target = directory.resolve("jdk"); installer.install("jdk", target); assertTrue(Files.exists(target.resolve("jdk/bin/javac")));
        Files.delete(archive); assertEquals(target, installer.install("jdk", target));
    }
    @Test void archivePathsCannotEscapeInstallationDirectory() throws Exception {
        Path archive = directory.resolve("bad.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) { zip.putNextEntry(new ZipEntry("../escape")); zip.write(1); zip.closeEntry(); }
        assertThrows(IOException.class, () -> Archives.extract(archive, directory.resolve("install")));
        assertFalse(Files.exists(directory.resolve("escape")));
    }
}
