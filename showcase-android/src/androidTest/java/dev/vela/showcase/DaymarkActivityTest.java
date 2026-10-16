package dev.vela.showcase;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("deprecation")
public final class DaymarkActivityTest extends InstrumentationTestCase {
    public void testStateAndNativeViewsSurviveUpdatesAndActivityRecreation() throws Exception {
        Intent intent = new Intent(getInstrumentation().getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Activity original = getInstrumentation().startActivitySync(intent);
        Activity restored = null;
        Instrumentation.ActivityMonitor monitor = null;
        try {
            AtomicReference<View> button = new AtomicReference<>();
            ui(() -> {
                View title = find(original.getWindow().getDecorView(), "DAYMARK");
                assertNotNull(title);
                int[] location = new int[2]; title.getLocationOnScreen(location);
                if (title.getRootWindowInsets() != null) {
                    assertTrue("App bar overlaps the status bar", location[1] >= title.getRootWindowInsets().getSystemWindowInsetTop());
                }
                assertNotNull(find(original.getWindow().getDecorView(), "5 / 8 glasses"));
                button.set(find(original.getWindow().getDecorView(), "+ Glass"));
                assertNotNull(button.get()); button.get().performClick();
            });
            awaitText(original, "6 / 8 glasses");
            ui(() -> assertSame(button.get(), find(original.getWindow().getDecorView(), "+ Glass")));
            monitor = getInstrumentation().addMonitor(MainActivity.class.getName(), null, false);
            ui(original::recreate);
            restored = monitor.waitForActivityWithTimeout(5000);
            assertNotNull("Activity recreation timed out", restored);
            assertNotSame(original, restored);
            awaitText(restored, "6 / 8 glasses");
        } finally {
            if (monitor != null) getInstrumentation().removeMonitor(monitor);
            Activity current = restored == null ? original : restored;
            ui(current::finish);
        }
    }

    private void awaitText(Activity activity, String text) throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + 4000;
        AtomicReference<View> found = new AtomicReference<>();
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            ui(() -> found.set(find(activity.getWindow().getDecorView(), text)));
            if (found.get() != null) return;
            Thread.sleep(25);
        }
        fail("Missing text after frame update: " + text);
    }

    private View find(View root, String text) {
        if (root instanceof TextView label && label.getText().toString().equals(text)) return root;
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void ui(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        getInstrumentation().runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() != null) throw new AssertionError("UI assertion failed", failure.get());
    }
}
