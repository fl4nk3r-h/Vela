package dev.vela.showcase;

import dev.vela.android.VelaActivity;
import dev.vela.core.VelaComponent;

public final class MainActivity extends VelaActivity {
    @Override protected VelaComponent createScreen() { return new DaymarkScreen(); }
    @Override protected String screenBackground() { return "#F4F6F2"; }
}
