package dev.vela.showcase;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import dev.vela.android.AndroidFrameScheduler;
import dev.vela.android.AndroidState;
import dev.vela.android.VelaAndroidBackend;
import dev.vela.core.VelaRuntime;

public final class MainActivity extends Activity {
    private FrameLayout host;
    private DaymarkScreen screen;
    private AndroidFrameScheduler scheduler;
    private VelaAndroidBackend backend;
    private VelaRuntime<VelaAndroidBackend.Handle> runtime;

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

        screen = new DaymarkScreen();
        if (savedInstanceState != null && savedInstanceState.getBundle("vela.state") != null) {
            screen.restoreState(AndroidState.decode(savedInstanceState.getBundle("vela.state")));
        }
        scheduler = new AndroidFrameScheduler();
        backend = new VelaAndroidBackend(this, host);
        runtime = new VelaRuntime<>(screen, backend, scheduler);
        runtime.mount();
        if (savedInstanceState != null && savedInstanceState.getBundle("vela.views") != null) {
            backend.restoreViewState(savedInstanceState.getBundle("vela.views"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBundle("vela.state", AndroidState.encode(screen.saveState()));
        outState.putBundle("vela.views", backend.saveViewState());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        try {
            if (runtime != null) runtime.close();
        } finally {
            try { if (backend != null) backend.close(); }
            finally {
                if (scheduler != null) scheduler.close();
                super.onDestroy();
            }
        }
    }
}
