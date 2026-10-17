package dev.vela.devtools;

import android.net.*;
import android.os.*;
import android.util.Log;
import android.system.Os;
import android.system.OsConstants;
import android.system.ErrnoException;
import dalvik.system.InMemoryDexClassLoader;
import dev.vela.android.*;
import dev.vela.core.VelaComponent;
import dev.vela.dev.ReloadProtocol;
import dev.vela.dev.ReloadClasses;
import dev.vela.resources.ResourceCatalog;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/** ADB-only, debug-only code loading; never added to the application's release dependency graph. */
public final class DevelopmentSession implements AutoCloseable {
    private record Environment(Class<? extends VelaComponent> screen, ResourceCatalog resources) { }
    private static final Map<String, Environment> environments = new ConcurrentHashMap<>();
    private final VelaActivity activity;
    private final String token;
    private final LocalServerSocket server;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Set<LocalSocket> clients = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean closed;

    /** Retain only the latest code/resources, never an Activity, across configuration changes. */
    public static ResourceCatalog resources(VelaActivity activity) {
        Environment environment = environments.get(activity.getClass().getName());
        return environment == null ? null : environment.resources();
    }
    public static VelaComponent screen(VelaActivity activity) throws ReflectiveOperationException {
        Environment environment = environments.get(activity.getClass().getName());
        return environment == null || environment.screen() == null ? null : environment.screen().getConstructor().newInstance();
    }

    public static AutoCloseable attach(VelaActivity activity) throws IOException {
        String token;
        try (InputStream in = activity.getAssets().open("vela/token")) {
            byte[] buffer = new byte[128]; int count = in.read(buffer);
            if (count < 32 || count == buffer.length) throw new IOException("Invalid development token");
            token = new String(buffer, 0, count, StandardCharsets.US_ASCII).trim();
        } catch (FileNotFoundException missing) { return () -> { }; }
        DevelopmentSession session = new DevelopmentSession(activity, token);
        session.restoreCheckpoint();
        Thread accept = new Thread(session::accept, "vela-development"); accept.setDaemon(true); accept.start();
        return session;
    }

    private DevelopmentSession(VelaActivity activity, String token) throws IOException {
        this.activity = activity; this.token = token;
        server = new LocalServerSocket("vela." + activity.getPackageName());
    }
    private void accept() {
        while (!closed) try {
            LocalSocket client = server.accept(); clients.add(client);
            Thread worker = new Thread(() -> handle(client), "vela-development-request"); worker.setDaemon(true); worker.start();
        } catch (IOException error) { if (!closed) Log.w("Vela", "Development connection failed", error); }
    }
    private void handle(LocalSocket client) {
        try (client) {
            client.setSoTimeout(15000);
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            ReloadProtocol.Request request = ReloadProtocol.readRequest(in, token);
            if (closed) return;
            if (request.operation() == ReloadProtocol.Operation.PING) {
                ReloadProtocol.writeResponse(out, new ReloadProtocol.Response(true, "ready")); return;
            }
            ResourceCatalog catalog = request.resources().length == 0 ? null : ResourceCatalog.decode(request.resources());
            ClassLoader loader = request.operation() == ReloadProtocol.Operation.CODE
                    ? new InMemoryDexClassLoader(ByteBuffer.wrap(request.dex()), new SharedParent(activity.getClassLoader(), request.classes())) : null;
            CountDownLatch done = new CountDownLatch(1);
            ReloadProtocol.Response[] response = new ReloadProtocol.Response[1];
            ui.post(() -> {
                try {
                    if (closed) throw new IllegalStateException("Development session closed");
                    switch (request.operation()) {
                        case CODE -> {
                            if (!request.entry().startsWith(activity.getPackageName() + ".")) throw new IllegalArgumentException("Entry screen is outside the reloadable namespace");
                            if (!request.classes().contains(request.entry())) throw new IllegalArgumentException("Reload payload is missing its entry class");
                            Class<? extends VelaComponent> screen = loader.loadClass(request.entry()).asSubclass(VelaComponent.class);
                            List<String> diagnostics = activity.replaceScreen(() -> {
                                try { return screen.getConstructor().newInstance(); }
                                catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot construct replacement screen", error); }
                            }, catalog == null ? activity.resourceCatalog() : catalog);
                            environments.put(activity.getClass().getName(), new Environment(screen, activity.resourceCatalog()));
                            response[0] = new ReloadProtocol.Response(true, "Reloaded" + (diagnostics.isEmpty() ? "" : ": " + String.join("; ", diagnostics)));
                        }
                        case RESOURCES -> {
                            activity.updateResources(Objects.requireNonNull(catalog, "resource catalog"));
                            environments.compute(activity.getClass().getName(), (key, previous) -> new Environment(previous == null ? null : previous.screen(), catalog));
                            response[0] = new ReloadProtocol.Response(true, "Resources refreshed");
                        }
                        case CHECKPOINT -> { checkpoint(); response[0] = new ReloadProtocol.Response(true, "Saved state for restart"); }
                        default -> throw new IllegalArgumentException("Unsupported development operation");
                    }
                } catch (Throwable error) {
                    Log.e("Vela", "Reload rejected; previous screen remains active", error);
                    response[0] = new ReloadProtocol.Response(false, error.getClass().getSimpleName() + ": " + Objects.toString(error.getMessage(), "Reload failed"));
                } finally { done.countDown(); }
            });
            if (!done.await(15, TimeUnit.SECONDS)) throw new IOException("UI thread did not respond to reload");
            ReloadProtocol.writeResponse(out, response[0]);
        } catch (Exception error) { if (!closed) Log.w("Vela", "Development request failed", error); }
        finally { clients.remove(client); }
    }
    private File checkpointFile() { return new File(activity.getFilesDir(), "vela-restart.state"); }
    private void checkpoint() throws IOException {
        Bundle data = new Bundle();
        data.putBundle("state", AndroidState.encode(activity.screen().saveState())); data.putBundle("views", activity.saveNativeState());
        Parcel parcel = Parcel.obtain();
        try { parcel.writeBundle(data); Files.write(checkpointFile().toPath(), parcel.marshall()); }
        finally { parcel.recycle(); }
    }
    private void restoreCheckpoint() {
        File file = checkpointFile(); if (!file.exists()) return;
        Parcel parcel = Parcel.obtain();
        try {
            if (file.length() > 16 * 1024 * 1024) throw new IOException("Restart checkpoint is too large");
            byte[] bytes = Files.readAllBytes(file.toPath());
            parcel.unmarshall(bytes, 0, bytes.length); parcel.setDataPosition(0);
            Bundle data = parcel.readBundle(getClass().getClassLoader());
            if (data != null && data.getBundle("state") != null) activity.restoreDevelopmentState(AndroidState.decode(data.getBundle("state")), data.getBundle("views"));
        } catch (Exception error) { Log.w("Vela", "Cannot restore restart checkpoint", error); }
        finally { parcel.recycle(); if (!file.delete()) Log.w("Vela", "Cannot remove restart checkpoint"); }
    }
    @Override public void close() throws IOException {
        closed = true;
        // Closing a descriptor on another thread does not cancel Linux's blocked accept.
        // Shutdown releases that syscall and the abstract socket name before recreation.
        try { Os.shutdown(server.getFileDescriptor(), OsConstants.SHUT_RDWR); }
        catch (ErrnoException error) { if (error.errno != OsConstants.ENOTCONN) Log.w("Vela", "Development socket shutdown failed", error); }
        finally { server.close(); }
        synchronized (clients) { for (LocalSocket client : clients) try { client.close(); } catch (IOException ignored) { } clients.clear(); }
    }
    private static final class SharedParent extends ClassLoader {
        private final ReloadClasses classes;
        private final ClassLoader shared;
        SharedParent(ClassLoader shared, List<String> definitions) { super(null); this.shared = shared; this.classes = new ReloadClasses(definitions); }
        @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // Make the DEX loader's normal parent-first lookup miss application classes.
            // Platform resource IDs remain shared with the installed APK.
            if (classes.owns(name)) throw new ClassNotFoundException(name);
            return shared.loadClass(name);
        }
    }
}
