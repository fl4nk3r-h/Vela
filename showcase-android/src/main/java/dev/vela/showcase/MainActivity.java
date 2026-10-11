package dev.vela.showcase;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

public final class MainActivity extends Activity {
    private FrameLayout host;
    private DaymarkScreen screen;
    private boolean renderScheduled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#F4F6F2"));
        window.setNavigationBarColor(Color.WHITE);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        host = new FrameLayout(this);
        host.setBackgroundColor(Color.parseColor("#F4F6F2"));
        setContentView(host, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        screen = new DaymarkScreen(this::scheduleRender);
        renderNow();
    }

    private void scheduleRender() {
        if (renderScheduled) {
            return;
        }
        renderScheduled = true;
        host.post(() -> {
            renderScheduled = false;
            if (screen.consumeDirty()) {
                renderNow();
            }
        });
    }

    private void renderNow() {
        View rendered = new VelaAndroidRenderer(this).render(screen.render());
        host.removeAllViews();
        host.addView(rendered, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }
}
