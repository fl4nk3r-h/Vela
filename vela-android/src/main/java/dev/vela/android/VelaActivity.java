package dev.vela.android;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import dev.vela.core.*;
import dev.vela.resources.*;
import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/** Lifecycle host for ordinary applications and transactional development screen replacement. */
public class VelaActivity extends Activity {
    private FrameLayout host;
    private Surface surface;
    private ResourceCatalog catalog = ResourceCatalog.empty();
    private AutoCloseable development;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        host = new FrameLayout(this);
        host.setBackgroundColor(Color.parseColor(screenBackground()));
        host.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        getWindow().setStatusBarColor(Color.parseColor(screenBackground()));
        getWindow().setNavigationBarColor(Color.parseColor(screenBackground()));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(host); host.requestApplyInsets();
        try (InputStream in = getAssets().open("vela/values.bin")) { catalog = ResourceCatalog.decode(read(in, ResourceCatalog.MAX_BYTES)); }
        catch (FileNotFoundException ignored) { }
        catch (IOException error) { throw new IllegalStateException("Cannot read Vela resources", error); }
        ResourceCatalog developmentCatalog = (ResourceCatalog) developmentValue("resources");
        if (developmentCatalog != null) catalog = developmentCatalog;
        VelaComponent screen = (VelaComponent) developmentValue("screen");
        if (screen == null) screen = createScreen();
        if (saved != null && saved.getBundle("vela.state") != null) screen.restoreState(AndroidState.decode(saved.getBundle("vela.state")), message -> Log.w("Vela", message));
        surface = mount(screen, catalog);
        host.addView(surface.host);
        if (saved != null && saved.getBundle("vela.views") != null) surface.backend.restoreViewState(saved.getBundle("vela.views"));
        startDevelopment();
    }

    protected String screenBackground() { return "#FFFFFF"; }

    protected VelaComponent createScreen() {
        try {
            ApplicationInfo app = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String entry = app.metaData == null ? null : app.metaData.getString("dev.vela.entry");
            if (entry == null) throw new IllegalStateException("Specify an entry screen in vela.yaml or override createScreen()");
            return (VelaComponent) getClassLoader().loadClass(entry).getConstructor().newInstance();
        } catch (ReflectiveOperationException | PackageManager.NameNotFoundException error) {
            throw new IllegalStateException("Cannot create Vela entry screen", error);
        }
    }

    public final VelaComponent screen() { return surface.screen; }
    public final VelaRuntime<VelaAndroidBackend.Handle> runtime() { return surface.runtime; }
    public final ResourceCatalog resourceCatalog() { return catalog; }
    public final Bundle saveNativeState() { checkUi(); return surface.backend.saveViewState(); }
    public final void restoreDevelopmentState(StateSnapshot state, Bundle nativeState) {
        checkUi();
        surface.screen.restoreState(state, message -> Log.w("Vela", message));
        surface.runtime.requestRender(); surface.runtime.flush();
        if (nativeState != null) surface.backend.restoreViewState(nativeState);
    }

    /** The old surface remains mounted until the replacement constructs and renders successfully. */
    public final List<String> replaceScreen(Supplier<VelaComponent> factory, ResourceCatalog resources) {
        checkUi();
        List<String> diagnostics = new ArrayList<>();
        StateSnapshot state = surface.screen.saveState();
        Bundle views = surface.backend.saveViewState();
        VelaComponent screen = Objects.requireNonNull(factory.get(), "replacement screen");
        screen.restoreState(state, diagnostics::add);
        Surface next = mount(screen, Objects.requireNonNull(resources, "resources"));
        try { next.backend.restoreViewState(views); }
        catch (RuntimeException | Error error) { next.close(); throw error; }
        Surface previous = surface;
        host.removeView(previous.host); host.addView(next.host);
        surface = next; catalog = resources;
        try { previous.close(); }
        catch (RuntimeException | Error error) { diagnostics.add("Previous screen cleanup failed; replacement is active"); Log.e("Vela", "Screen cleanup failed", error); }
        return diagnostics;
    }

    public final void updateResources(ResourceCatalog next) {
        checkUi();
        ResourceCatalog previous = catalog;
        try {
            surface.runtime.updateResources(resolver(next)); surface.runtime.flush(); catalog = next;
        } catch (RuntimeException | Error error) {
            try { surface.runtime.updateResources(resolver(previous)); surface.runtime.flush(); }
            catch (RuntimeException | Error recovery) { error.addSuppressed(recovery); }
            throw error;
        }
    }

    private ResourceResolver resolver(ResourceCatalog source) {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        return new ResourceResolver(source, locale, "default");
    }

    private Surface mount(VelaComponent screen, ResourceCatalog resources) {
        Surface next = new Surface(screen, resources);
        try { next.runtime.mount(); return next; }
        catch (RuntimeException | Error error) {
            try { next.close(); } catch (RuntimeException | Error cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    private void startDevelopment() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return;
        try {
            Class<?> bridge = Class.forName("dev.vela.devtools.DevelopmentSession");
            development = (AutoCloseable) bridge.getMethod("attach", VelaActivity.class).invoke(null, this);
        } catch (ClassNotFoundException ignored) { }
        catch (ReflectiveOperationException error) { Log.w("Vela", "Cannot start development session", error); }
    }

    private Object developmentValue(String method) {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null;
        try {
            Class<?> bridge = Class.forName("dev.vela.devtools.DevelopmentSession");
            return bridge.getMethod(method, VelaActivity.class).invoke(null, this);
        } catch (ClassNotFoundException ignored) { return null; }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot restore development environment", error); }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        if (surface != null) {
            out.putBundle("vela.state", AndroidState.encode(surface.screen.saveState()));
            out.putBundle("vela.views", surface.backend.saveViewState());
        }
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        try {
            if (development != null) try { development.close(); }
                catch (Exception error) { Log.w("Vela", "Development shutdown failed", error); }
        } finally {
            try { if (surface != null) surface.close(); }
            finally { super.onDestroy(); }
        }
    }
    private void checkUi() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) throw new IllegalStateException("Access VelaActivity on the UI thread");
    }
    private static byte[] read(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int size;
        while ((size = in.read(buffer)) != -1) {
            if (out.size() + size > limit) throw new IOException("Resource catalog too large");
            out.write(buffer, 0, size);
        }
        return out.toByteArray();
    }
    private final class Surface implements AutoCloseable {
        final FrameLayout host = new FrameLayout(VelaActivity.this);
        final AndroidFrameScheduler scheduler = new AndroidFrameScheduler();
        final VelaAndroidBackend backend = new VelaAndroidBackend(VelaActivity.this, host);
        final VelaComponent screen;
        final VelaRuntime<VelaAndroidBackend.Handle> runtime;
        Surface(VelaComponent screen, ResourceCatalog resources) {
            this.screen = screen;
            host.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            runtime = new VelaRuntime<>(screen, backend, scheduler, resolver(resources));
        }
        @Override public void close() {
            try { runtime.close(); }
            finally { try { backend.close(); } finally { scheduler.close(); } }
        }
    }
}
