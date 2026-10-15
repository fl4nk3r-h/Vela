package dev.vela.android;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.test.InstrumentationTestCase;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.vela.components.*;
import dev.vela.core.*;
import dev.vela.ui.*;

@SuppressWarnings("deprecation")
public final class AndroidBackendTest extends InstrumentationTestCase {
    private TestActivity activity;
    private FrameLayout host;
    private VelaAndroidBackend backend;
    private MountedTree<VelaAndroidBackend.Handle> tree;

    @Override protected void setUp() throws Exception {
        super.setUp();
        Intent intent = new Intent(getInstrumentation().getTargetContext(), TestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = (TestActivity) getInstrumentation().startActivitySync(intent);
        ui(() -> {
            host = new FrameLayout(activity); activity.setContentView(host);
            backend = new VelaAndroidBackend(activity, host);
            tree = new MountedTree<>(backend);
        });
    }

    @Override protected void tearDown() throws Exception {
        try { ui(() -> { tree.close(); backend.close(); activity.finish(); }); }
        finally { super.tearDown(); }
    }

    public void testRetainsEditorFocusSelectionAndFreshCallbacks() throws Exception {
        ui(() -> {
            AtomicReference<String> value = new AtomicReference<>("abcdef");
            tree.render(field("abcdef", ignored -> value.set("old")));
            EditText edit = (EditText) tree.root().widget();
            edit.requestFocus(); edit.setSelection(2, 4);
            tree.render(field("abcdef", value::set));
            assertSame(edit, tree.root().widget());
            assertTrue(edit.hasFocus()); assertEquals(2, edit.getSelectionStart()); assertEquals(4, edit.getSelectionEnd());
            edit.setText("new text");
            assertEquals("new text", value.get());
        });
    }

    public void testRetainsScrollPositionAcrossUpdates() throws Exception {
        ui(() -> {
            tree.render(scrolling("before")); measure();
            ScrollView scroll = (ScrollView) tree.root().widget(); scroll.scrollTo(0, 100);
            assertEquals(100, scroll.getScrollY());
            tree.render(scrolling("after")); measure();
            assertSame(scroll, tree.root().widget()); assertEquals(100, scroll.getScrollY());
        });
    }

    public void testAccessibilityAndKeyboardClicksUpdateCheckboxAndSwitch() throws Exception {
        ui(() -> {
            AtomicInteger calls = new AtomicInteger();
            tree.render(VelaCheckbox.of("Check", false, true, checked -> calls.incrementAndGet(), Modifier.none()));
            CheckBox box = (CheckBox) tree.root().widget();
            assertFalse(box.isPressed());
            assertTrue(box.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null));
            assertEquals(1, calls.get());
            tree.render(VelaSwitch.of("Switch", false, true, checked -> calls.incrementAndGet(), Modifier.none()));
            Switch control = (Switch) tree.root().widget();
            control.performClick();
            assertEquals(2, calls.get());
        });
    }

    public void testDisabledCompositeAndAncestorsSuppressCallbacks() throws Exception {
        ui(() -> {
            AtomicInteger calls = new AtomicInteger();
            tree.render(VelaSegmentedControl.of(List.of("a", "b"), "a", value -> calls.incrementAndGet(), Modifier.none().enabled(false)));
            VelaAndroidBackend.Handle option = tree.root().children().get(1);
            assertFalse(option.widget().isEnabled()); option.widget().performClick(); assertEquals(0, calls.get());
            tree.render(VelaColumn.of(Modifier.none().enabled(false), VelaButton.of("Action", calls::incrementAndGet)));
            tree.root().children().get(0).widget().performClick(); assertEquals(0, calls.get());
        });
    }

    public void testSegmentedCallbacksSurviveRetainedUpdates() throws Exception {
        ui(() -> {
            AtomicReference<String> result = new AtomicReference<>();
            tree.render(VelaSegmentedControl.of(List.of("a", "b"), "a", value -> result.set("old"), Modifier.none()));
            View option = tree.root().children().get(1).widget();
            tree.render(VelaSegmentedControl.of(List.of("a", "b"), "a", result::set, Modifier.none()));
            assertSame(option, tree.root().children().get(1).widget()); option.performClick(); assertEquals("b", result.get());
        });
    }

    public void testNativeSliderUsesStepsAndSurvivesStateDrivenUpdates() throws Exception {
        ui(() -> {
            class Screen extends VelaComponent {
                final MutableState<Double> value = state(0.0);
                @Override public ViewNode render() { return VelaSlider.of("Count", value.get(), 0, 10, 2, value::set, Modifier.none()); }
            }
            Screen screen = new Screen();
            var frames = new ArrayDeque<Runnable>();
            try (var runtime = new VelaRuntime<>(screen, backend, frames::addLast)) {
                runtime.mount();
                SeekBar bar = (SeekBar) runtime.root().widget(); bar.setKeyProgressIncrement(1);
                assertEquals(5, bar.getMax());
                bar.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                assertEquals(2.0, screen.value.get());
                runtime.flush(); assertSame(bar, runtime.root().widget());
                bar.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                assertEquals(4.0, screen.value.get());
                runtime.flush(); assertSame(bar, runtime.root().widget());
            }
        });
    }

    public void testRealEditorsSelectorsAndModalDismissal() throws Exception {
        ui(() -> {
            AtomicInteger submits = new AtomicInteger();
            tree.render(VelaForm.of("Account", submits::incrementAndGet, Modifier.none(), field("", ignored -> { })));
            EditText edit = (EditText) tree.root().children().get(0).widget();
            edit.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE); assertEquals(1, submits.get());
            tree.render(VelaSelect.of("Mode", "b", List.of("a", "b"), ignored -> { }, Modifier.none()));
            Spinner select = (Spinner) tree.root().widget(); assertEquals("b", select.getSelectedItem());
            tree.render(VelaDatePicker.of("Date", LocalDate.of(2026, 1, 1), null, null, ignored -> { }, Modifier.none()));
            assertTrue(tree.root().widget() instanceof Button);
            AtomicInteger dismissals = new AtomicInteger();
            tree.render(VelaDialog.of("Confirm", VelaText.of("Content"), null, dismissals::incrementAndGet, Modifier.none()));
            assertTrue(tree.root().dialog().isShowing()); tree.root().dialog().dismiss();
        });
    }

    public void testImagesUseSourcesRespectScaleAndCancelStaleLoads() throws Exception {
        ui(() -> {
            AtomicInteger cancelled = new AtomicInteger();
            var callbacks = new java.util.ArrayList<java.util.function.Consumer<android.graphics.drawable.Drawable>>();
            ImageLoader loader = (source, result, failure) -> { callbacks.add(result); return cancelled::incrementAndGet; };
            tree.close(); backend.close(); backend = new VelaAndroidBackend(activity, host, loader); tree = new MountedTree<>(backend);
            tree.render(VelaImage.of("first", "Image", VelaImage.Scale.FIT, Modifier.none()));
            ImageView image = (ImageView) tree.root().widget();
            tree.render(VelaImage.of("second", "Image", VelaImage.Scale.CROP, Modifier.none()));
            assertEquals(1, cancelled.get());
            callbacks.get(0).accept(new ColorDrawable(Color.RED)); assertNull(image.getDrawable());
            callbacks.get(1).accept(new ColorDrawable(Color.BLUE));
            assertEquals(Color.BLUE, ((ColorDrawable) image.getDrawable()).getColor());
            assertEquals(ImageView.ScaleType.CENTER_CROP, image.getScaleType());
            tree.close(); assertEquals(2, cancelled.get());
        });
    }

    public void testModifiersMeasureBoundsApplyBorderAndDistributeSpace() throws Exception {
        ui(() -> {
            tree.render(VelaText.of("Bounded", TextStyle.defaultStyle(), Modifier.none().width(200).maxWidth(80)
                    .height(30).border(2, "#ff0000").cornerRadius(6).focusable(true)));
            measure();
            assertTrue(tree.root().view().getMeasuredWidth() <= pixels(80));
            assertNotNull(tree.root().view().getBackground()); assertTrue(tree.root().widget().isFocusable());
            tree.render(VelaRow.of(Arrangement.SPACE_EVENLY, Alignment.CENTER, 0, Modifier.none().width(300).height(60),
                    VelaText.of("A", TextStyle.defaultStyle(), Modifier.none().width(30)),
                    VelaText.of("B", TextStyle.defaultStyle(), Modifier.none().width(30))));
            measure();
            View first = tree.root().children().get(0).view(), second = tree.root().children().get(1).view();
            assertTrue(first.getLeft() > 0); assertTrue(second.getLeft() > first.getRight());
            tree.render(VelaBox.of(Alignment.START, Modifier.none().width(100),
                    VelaText.of("Ratio", TextStyle.defaultStyle(), Modifier.none().width(100).aspectRatio(2))));
            measure();
            View ratio = tree.root().children().get(0).view();
            assertEquals(ratio.getMeasuredWidth() / 2, ratio.getMeasuredHeight());
        });
    }

    public void testLazyListBuildsVisibleRowsAndRecycles() throws Exception {
        ui(() -> {
            AtomicInteger built = new AtomicInteger();
            tree.render(VelaLazyList.of(10_000, i -> "item-" + i, i -> {
                built.incrementAndGet(); return VelaText.of("Item " + i, TextStyle.defaultStyle(), Modifier.none().height(40));
            }, Modifier.none().fillMaxWidth().height(200)));
            assertEquals(0, built.get()); measure();
            ListView list = (ListView) tree.root().widget();
            assertTrue(built.get() > 0); assertTrue(built.get() < 100);
            View row = list.getAdapter().getView(123, null, list);
            assertSame(row, list.getAdapter().getView(124, row, list));
            assertEquals(10_000, list.getAdapter().getCount());
        });
    }

    public void testEveryCatalogNodeHasAnExplicitBackendAndUnknownTypesFail() throws Exception {
        ui(() -> {
            for (String type : VelaAndroidBackend.supportedTypes()) {
                ViewNode.Builder fixture = ViewNode.builder(type).prop("label", "Test");
                if (type.equals("date-picker")) fixture.prop("value", "2026-10-15");
                if (type.startsWith("lazy-")) fixture.lazyContent(new LazyContent(0, i -> "item-" + i, i -> VelaText.of("Item")));
                tree.render(fixture.build()); assertNotNull(type, tree.root().widget());
            }
            try { tree.render(ViewNode.builder("unsupported-chart").build()); fail("Expected unsupported node diagnostic"); }
            catch (UnsupportedOperationException expected) { assertTrue(expected.getMessage().contains("unsupported-chart")); }
        });
    }

    public void testStateSnapshotSurvivesParcelAndRestoresNativeSelection() throws Exception {
        final Bundle[] savedViews = new Bundle[1];
        ui(() -> {
            StateSnapshot snapshot = new StateSnapshot(java.util.Map.of("count", "7"), java.util.Map.of());
            Parcel parcel = Parcel.obtain();
            Bundle roundtrip;
            try { parcel.writeBundle(AndroidState.encode(snapshot)); parcel.setDataPosition(0); roundtrip = parcel.readBundle(); }
            finally { parcel.recycle(); }
            assertEquals(snapshot, AndroidState.decode(roundtrip));
            tree.render(field("abcdef", ignored -> { }));
            ((EditText) tree.root().widget()).setSelection(3); savedViews[0] = backend.saveViewState();
            tree.close(); backend.close(); backend = new VelaAndroidBackend(activity, host); tree = new MountedTree<>(backend);
            tree.render(field("abcdef", ignored -> { })); backend.restoreViewState(savedViews[0]);
        });
        getInstrumentation().waitForIdleSync();
        ui(() -> assertEquals(3, ((EditText) tree.root().widget()).getSelectionStart()));
    }

    private ViewNode field(String value, java.util.function.Consumer<String> changed) {
        return VelaTextField.of("Name", value, "Type here", VelaTextField.InputType.TEXT, true, changed, Modifier.none()).withKey("field");
    }
    private ViewNode scrolling(String value) {
        return VelaScroll.of(Orientation.VERTICAL, Modifier.none().height(200).fillMaxWidth(), VelaColumn.of(
                VelaText.of("Tall", TextStyle.defaultStyle(), Modifier.none().height(1000)), VelaText.of(value)));
    }
    private int pixels(int dp) { return Math.round(dp * activity.getResources().getDisplayMetrics().density); }
    private void measure() {
        host.measure(View.MeasureSpec.makeMeasureSpec(pixels(400), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(pixels(600), View.MeasureSpec.EXACTLY));
        host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
    }
    private void ui(Runnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        getInstrumentation().runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() != null) throw new AssertionError("UI assertion failed", failure.get());
    }
}
