package dev.vela.android;

import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import dev.vela.resources.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("deprecation")
public final class VelaActivityTest extends InstrumentationTestCase {
    public void testReplacementPreservesStateAndFailureKeepsWorkingScreen() throws Exception {
        HostTestActivity.active.set(0); HostTestActivity.disposed.set(0);
        Intent intent = new Intent(getInstrumentation().getTargetContext(), HostTestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        HostTestActivity activity = (HostTestActivity) getInstrumentation().startActivitySync(intent);
        try {
            ui(() -> {
                find(activity.getWindow().getDecorView(), "Increment").performClick(); activity.runtime().flush();
                EditText field = edit(activity.getWindow().getDecorView()); field.setText("Ada"); field.requestFocus(); field.setSelection(2);
                activity.runtime().flush();
                activity.replaceScreen(() -> new HostTestActivity.Screen("Changed"), ResourceCatalog.empty());
            });
            getInstrumentation().waitForIdleSync();
            ui(() -> {
                assertNotNull(find(activity.getWindow().getDecorView(), "Changed 1"));
                EditText field = edit(activity.getWindow().getDecorView()); assertEquals("Ada", field.getText().toString()); assertEquals(2, field.getSelectionStart());
                assertEquals(1, HostTestActivity.active.get()); assertEquals(1, HostTestActivity.disposed.get());
                try { activity.replaceScreen(() -> new HostTestActivity.Screen("Broken"), ResourceCatalog.empty()); fail("Expected render failure"); }
                catch (IllegalStateException expected) { assertEquals("Broken candidate", expected.getMessage()); }
                assertNotNull(find(activity.getWindow().getDecorView(), "Changed 1")); assertEquals(1, HostTestActivity.active.get());
                for (int i = 0; i < 5; i++) activity.replaceScreen(() -> new HostTestActivity.Screen("Repeated"), ResourceCatalog.empty());
                assertEquals(1, HostTestActivity.active.get()); assertNotNull(find(activity.getWindow().getDecorView(), "Repeated 1"));
            });
        } finally { ui(activity::finish); getInstrumentation().waitForIdleSync(); }
        awaitDisposed();
    }
    public void testResourceRefreshUsesCurrentLocaleWithoutReplacingComponent() {
        Intent intent = new Intent(getInstrumentation().getTargetContext(), HostTestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        HostTestActivity activity = (HostTestActivity) getInstrumentation().startActivitySync(intent);
        try {
            ui(() -> {
                var component = new dev.vela.core.VelaComponent() {
                    @Override public dev.vela.core.ViewNode render() { return dev.vela.ui.UI.text(resources().string("title")); }
                };
                activity.replaceScreen(() -> component, new ResourceCatalog(Map.of("", Map.of("strings.title", "Before"))));
                activity.updateResources(new ResourceCatalog(Map.of("", Map.of("strings.title", "After"))));
                assertSame(component, activity.screen()); assertNotNull(find(activity.getWindow().getDecorView(), "After"));
            });
        } finally { ui(activity::finish); awaitDisposed(); }
    }
    private View find(View view, String text) {
        if (view instanceof TextView label && label.getText().toString().equals(text)) return view;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { View found = find(group.getChildAt(i), text); if (found != null) return found; }
        return null;
    }
    private EditText edit(View view) {
        if (view instanceof EditText field) return field;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { EditText found = edit(group.getChildAt(i)); if (found != null) return found; }
        return null;
    }
    private void ui(Runnable action) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        getInstrumentation().runOnMainSync(() -> { try { action.run(); } catch (Throwable failure) { error.set(failure); } });
        if (error.get() != null) throw new AssertionError("UI assertion failed", error.get());
    }
    private void awaitDisposed() {
        long deadline = android.os.SystemClock.uptimeMillis() + 4000;
        while (HostTestActivity.active.get() != 0 && android.os.SystemClock.uptimeMillis() < deadline) {
            android.os.SystemClock.sleep(25);
        }
        assertEquals(0, HostTestActivity.active.get());
    }
}
