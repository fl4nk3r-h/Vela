package dev.vela.android;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import dev.vela.core.*;
import dev.vela.ui.SliderRange;

/** A retained native backend. One handle owns one widget plus a consistent modifier boundary. */
public final class VelaAndroidBackend implements RendererBackend<VelaAndroidBackend.Handle>, AutoCloseable {
    private static final int INK = Color.rgb(23, 33, 29), MUTED = Color.rgb(99, 112, 105);
    private static final int BORDER = Color.rgb(220, 226, 222), GREEN = Color.rgb(35, 122, 87);
    private static final Set<String> TYPES = Set.of(
            "accordion", "accordion-item", "alert", "avatar", "badge", "box", "breadcrumb", "breadcrumbs",
            "button", "card", "checkbox", "chip", "code-block", "column", "component", "date-picker", "dialog",
            "divider", "drawer", "empty-state", "floating-action-button", "form", "grid", "icon", "icon-button",
            "image", "key-value-item", "key-value-list", "lazy-grid", "lazy-list", "link", "list", "list-item",
            "menu", "menu-item", "navigation-bar", "navigation-item", "option", "pagination", "progress",
            "radio-button", "row", "scaffold", "scroll", "segment", "segmented-control", "select", "skeleton",
            "slider", "slot", "snackbar", "spacer", "surface", "switch", "tab", "table", "table-cell",
            "table-row", "tabs", "text", "text-field", "tooltip", "top-app-bar");
    private static final Set<String> MODIFIERS = Set.of(
            "style.width", "style.height", "style.minWidth", "style.maxWidth", "style.minHeight", "style.maxHeight",
            "style.padding", "style.margin", "style.background", "style.foreground", "style.border", "style.cornerRadius",
            "style.shadow", "style.opacity", "style.aspectRatio", "layout.weight", "layout.alignSelf", "layout.zIndex",
            "layout.arrangement", "layout.alignment", "layout.gap", "layout.contentAlignment", "layout.columns",
            "state.visible", "state.enabled", "interaction.focusable", "interaction.scrollable",
            "semantics.contentDescription", "semantics.role", "semantics.testTag", "semantics.sensitive",
            "text.fontSize", "text.fontWeight", "text.color", "text.fontFamily", "text.align", "text.lineHeight",
            "text.italic", "text.underline", "text.maxLines");

    private final Context context;
    private final ViewGroup container;
    private final ImageLoader images;
    private final boolean ownsImages;
    private final Set<Handle> live = Collections.newSetFromMap(new IdentityHashMap<>());
    private Handle root;
    private boolean closed;

    public VelaAndroidBackend(Context context, ViewGroup container) {
        this(context, container, new AndroidImageLoader(context), true);
    }

    public VelaAndroidBackend(Context context, ViewGroup container, ImageLoader images) {
        this(context, container, images, false);
    }

    private VelaAndroidBackend(Context context, ViewGroup container, ImageLoader images, boolean ownsImages) {
        checkThread();
        this.context = Objects.requireNonNull(context, "context");
        this.container = Objects.requireNonNull(container, "container");
        this.images = Objects.requireNonNull(images, "images");
        this.ownsImages = ownsImages;
    }

    public static Set<String> supportedTypes() { return TYPES; }

    public Bundle saveViewState() {
        checkOpen();
        return root == null ? new Bundle() : saveViewState(root);
    }

    private Bundle saveViewState(Handle h) {
        Bundle state = new Bundle();
        state.putString("type", h.node.type());
        state.putInt("scrollX", h.widget.getScrollX()); state.putInt("scrollY", h.widget.getScrollY());
        state.putBoolean("focused", h.widget.isFocused());
        if (h.widget instanceof EditText edit) {
            state.putInt("selectionStart", edit.getSelectionStart()); state.putInt("selectionEnd", edit.getSelectionEnd());
        }
        Bundle children = new Bundle();
        for (int i = 0; i < h.children.size(); i++) {
            Handle child = h.children.get(i);
            children.putBundle(child.node.key() == null ? "index:" + i : "key:" + child.node.key(), saveViewState(child));
        }
        state.putBundle("children", children);
        return state;
    }

    public void restoreViewState(Bundle state) {
        checkOpen();
        if (root != null) restoreViewState(root, Objects.requireNonNull(state, "state"));
    }

    private void restoreViewState(Handle h, Bundle state) {
        if (!h.node.type().equals(state.getString("type"))) return;
        h.restore = () -> {
            if (h.disposed) return;
            h.widget.scrollTo(state.getInt("scrollX"), state.getInt("scrollY"));
            if (state.getBoolean("focused")) h.widget.requestFocus();
            if (h.widget instanceof EditText edit) {
                int start = Math.max(0, Math.min(edit.length(), state.getInt("selectionStart")));
                int end = Math.max(0, Math.min(edit.length(), state.getInt("selectionEnd")));
                edit.setSelection(start, end);
            }
        };
        h.view.post(h.restore);
        Bundle children = state.getBundle("children");
        if (children != null) for (int i = 0; i < h.children.size(); i++) {
            Handle child = h.children.get(i);
            Bundle saved = children.getBundle(child.node.key() == null ? "index:" + i : "key:" + child.node.key());
            if (saved != null) restoreViewState(child, saved);
        }
    }

    @Override public Handle create(ViewNode node) {
        checkOpen(); validate(node);
        Handle h = new Handle(node, new BoundedLayout(context));
        live.add(h);
        try {
            buildWidget(h);
            bind(h, null);
            return h;
        } catch (RuntimeException | Error failure) {
            dispose(h);
            throw failure;
        }
    }

    @Override public void update(Handle h, ViewNode previous, ViewNode next) {
        checkOpen(); validate(next);
        h.node = next;
        boolean rebuilt = !shape(previous).equals(shape(next));
        if (rebuilt) {
            cancelImage(h);
            if (h.adapter != null) { h.adapter.close(); h.adapter = null; }
            dismissDialog(h);
            List<Handle> retained = h.children;
            if (h.target != null) for (Handle child : retained) h.target.removeView(child.view);
            h.view.removeAllViews();
            buildWidget(h);
            setChildren(h, retained);
        }
        bind(h, rebuilt ? null : previous);
        if (!previous.props().equals(next.props())) {
            layoutChildren(h);
            if (h.parent != null) layoutChildren(h.parent);
            else if (root == h) applyRootSize(h);
        }
    }

    private void buildWidget(Handle h) {
        h.title = null; h.detail = null; h.action = null; h.target = null;
        String type = h.node.type();
        h.widget = switch (type) {
            case "text", "table-cell", "option", "badge", "icon", "divider", "skeleton", "spacer" -> textOrSpace(type);
            case "avatar" -> "image".equals(h.node.props().get("kind")) ? new ImageView(context) : label();
            case "button", "icon-button", "floating-action-button", "link", "navigation-item", "tab", "menu-item",
                    "breadcrumb", "segment", "date-picker" -> button();
            case "checkbox" -> new CheckBox(context);
            case "switch" -> new Switch(context);
            case "radio-button" -> new RadioButton(context);
            case "slider" -> new SeekBar(context);
            case "text-field" -> new EditText(context);
            case "select" -> new Spinner(context);
            case "progress" -> "circular".equals(h.node.props().get("style")) ? new ProgressBar(context)
                    : new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            case "image" -> new ImageView(context);
            case "scroll" -> scroll(h);
            case "lazy-list" -> new ListView(context);
            case "lazy-grid" -> new GridView(context);
            case "box", "surface", "slot", "component", "tooltip" -> group(h, new FrameLayout(context));
            case "grid" -> group(h, new GridLayout(context));
            default -> panel(h);
        };
        View displayed = h.widget;
        String scrollable = h.node.props().get("interaction.scrollable");
        if (scrollable != null && !type.equals("scroll")) {
            FrameLayout scroller = scrollable.equals("horizontal") ? new HorizontalScrollView(context) : new ScrollView(context);
            scroller.addView(h.widget, new FrameLayout.LayoutParams(
                    scrollable.equals("horizontal") ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            displayed = scroller;
        }
        if (type.equals("dialog") || type.equals("drawer")) {
            h.dialog = new Dialog(context);
            h.dialog.setContentView(displayed);
            h.dialog.setOnDismissListener(dialog -> { if (!h.disposed && !h.applying) h.dispatch("dismiss", null); });
            h.view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) { showDialog(h); }
                @Override public void onViewDetachedFromWindow(View v) { dismissDialog(h); }
            });
        } else {
            h.view.addView(displayed, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        installListeners(h);
    }

    private View textOrSpace(String type) {
        return Set.of("divider", "skeleton", "spacer").contains(type) ? new View(context) : label();
    }

    private <T extends ViewGroup> T group(Handle h, T group) { h.target = group; return group; }

    private View scroll(Handle h) {
        FrameLayout scroll = "horizontal".equals(h.node.props().get("orientation"))
                ? new HorizontalScrollView(context) : new ScrollView(context);
        if (scroll instanceof ScrollView vertical) vertical.setFillViewport(true);
        if (scroll instanceof HorizontalScrollView horizontal) horizontal.setFillViewport(true);
        h.target = scroll;
        return scroll;
    }

    private View panel(Handle h) {
        String type = h.node.type();
        boolean horizontal = Set.of("row", "navigation-bar", "tabs", "breadcrumbs", "segmented-control", "table-row",
                "key-value-item", "top-app-bar", "list-item", "chip").contains(type);
        ArrangedLayout layout = new ArrangedLayout(context, horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        h.target = layout;
        if (Set.of("top-app-bar", "list-item", "alert", "snackbar", "empty-state", "dialog", "accordion-item",
                "code-block", "key-value-item", "chip").contains(type)) {
            h.title = label();
            layout.addView(h.title, new LinearLayout.LayoutParams(horizontal ? 0 : ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, horizontal ? 1 : 0));
        }
        if (Set.of("alert", "empty-state", "list-item", "key-value-item").contains(type)) {
            h.detail = label();
            h.detail.setTextColor(MUTED);
            layout.addView(h.detail);
        }
        if (Set.of("form", "snackbar", "alert", "chip", "code-block", "accordion-item").contains(type)) {
            h.action = button();
            layout.addView(h.action);
        }
        if (type.equals("pagination")) {
            h.title = label();
            Button previous = button(), next = button();
            previous.setText("Previous"); next.setText("Next");
            previous.setOnClickListener(v -> h.dispatch("change", integer(h.node, "currentPage", 1) - 1));
            next.setOnClickListener(v -> h.dispatch("change", integer(h.node, "currentPage", 1) + 1));
            h.previous = previous; h.next = next;
            layout.addView(previous); layout.addView(h.title); layout.addView(next);
        }
        return layout;
    }

    private void installListeners(Handle h) {
        if (h.widget instanceof CompoundButton control) {
            control.setOnCheckedChangeListener((button, checked) -> {
                if (!h.applying) {
                    if (h.node.type().equals("radio-button")) { if (checked) h.dispatch("select", null); }
                    else h.dispatch("change", checked);
                }
            });
        } else if (h.widget instanceof SeekBar bar) {
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
                    if (fromUser && !h.applying) h.dispatch("change", range(h.node).valueAt(progress));
                }
                @Override public void onStartTrackingTouch(SeekBar view) { }
                @Override public void onStopTrackingTouch(SeekBar view) { }
            });
        } else if (h.widget instanceof EditText edit) {
            edit.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!h.applying) h.dispatch("change", s.toString());
                }
                @Override public void afterTextChanged(Editable text) { }
            });
            edit.setOnEditorActionListener((view, action, event) -> {
                for (Handle scope = h; scope != null; scope = scope.parent) {
                    if (scope.node.eventNames().contains("submit")) return scope.dispatch("submit", null);
                }
                return false;
            });
        } else if (h.widget instanceof Spinner spinner) {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!h.applying && h.node != null) {
                        String selected = String.valueOf(parent.getItemAtPosition(position));
                        if (!selected.equals(h.node.props().get("selectedValue"))) h.dispatch("change", selected);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });
        }
        if (h.node.type().equals("date-picker")) {
            h.widget.setOnClickListener(v -> {
                if (!h.enabled()) return;
                LocalDate current = LocalDate.parse(h.node.props().get("value"));
                DatePickerDialog picker = new DatePickerDialog(context, (date, year, month, day) ->
                        h.dispatch("change", LocalDate.of(year, month + 1, day)), current.getYear(),
                        current.getMonthValue() - 1, current.getDayOfMonth());
                if (h.node.props().containsKey("minimum")) picker.getDatePicker().setMinDate(dateMillis(h.node.props().get("minimum")));
                if (h.node.props().containsKey("maximum")) picker.getDatePicker().setMaxDate(dateMillis(h.node.props().get("maximum")));
                h.picker = picker; picker.show();
            });
        }
    }

    private void bind(Handle h, ViewNode previous) {
        h.applying = true;
        try {
            ViewNode n = h.node; Map<String, String> props = n.props(); String type = n.type();
            if (h.widget instanceof EditText edit) {
                edit.setHint(props.getOrDefault("placeholder", ""));
                int input = inputType(props.getOrDefault("inputType", "text"));
                boolean multiline = bool(n, "multiline", false);
                if (multiline) input |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                if (edit.getInputType() != input) edit.setInputType(input);
                edit.setSingleLine(!multiline); edit.setMinLines(integer(n, "minimumLines", 1));
                edit.setContentDescription(props.getOrDefault("label", ""));
                String value = props.getOrDefault("value", "");
                if (!edit.getText().toString().equals(value)) {
                    int start = edit.getSelectionStart(), end = edit.getSelectionEnd();
                    edit.setText(value);
                    if (start >= 0 && end >= 0) edit.setSelection(Math.min(start, value.length()), Math.min(end, value.length()));
                }
            } else if (h.widget instanceof CompoundButton control) {
                control.setText(props.getOrDefault("label", ""));
                control.setChecked(bool(n, "checked", bool(n, "selected", false)));
            } else if (h.widget instanceof TextView text) {
                text.setText(textValue(n));
                styleText(text, props);
                if (h.widget instanceof Button) {
                    String variant = props.getOrDefault("variant", "primary");
                    text.setTextColor(Set.of("warning", "ghost", "outlined").contains(variant) ? INK : Color.WHITE);
                    text.setBackground(background(variantColor(variant), dp(8), BORDER, 0));
                    text.setSelected(bool(n, "selected", false));
                }
            }
            if (h.title != null) h.title.setText(title(n));
            if (h.detail != null) h.detail.setText(props.getOrDefault("supportingText", props.getOrDefault("message",
                    props.getOrDefault("value", ""))));
            if (type.equals("key-value-item")) h.title.setText(props.getOrDefault("key", ""));
            if (type.equals("pagination")) {
                int page = integer(n, "currentPage", 1), count = integer(n, "pageCount", 1);
                h.title.setText(page + " / " + count); h.previous.setEnabled(page > 1); h.next.setEnabled(page < count);
            }
            if (h.action != null) bindAction(h);
            if (h.widget instanceof SeekBar bar) {
                SliderRange range = range(n); bar.setMax(range.intervals());
                bar.setProgress(range.indexOf(number(n, "value", range.minimum())));
                bar.setContentDescription(props.get("label"));
            } else if (h.widget instanceof ProgressBar progress) {
                progress.setIndeterminate(bool(n, "indeterminate", false)); progress.setMax(1000);
                progress.setProgress((int) Math.round(number(n, "progress", 0) * 1000));
            }
            if (h.widget instanceof Spinner spinner) bindSpinner(h, spinner, previous);
            if (h.widget instanceof AbsListView list && n.lazyContent() != null) {
                if (h.adapter == null) { h.adapter = new LazyAdapter(h); list.setAdapter(h.adapter); }
                else h.adapter.notifyDataSetChanged();
                if (list instanceof GridView grid) grid.setNumColumns(integer(n, "layout.columns", 2));
            }
            if (h.widget instanceof ImageView image) bindImage(h, image, previous);
            if (h.widget instanceof ArrangedLayout linear) {
                linear.arrangement = props.getOrDefault("layout.arrangement", "start");
                String alignment = props.getOrDefault("layout.alignment", "start");
                int gravity = linear.getOrientation() == LinearLayout.HORIZONTAL ? verticalGravity(alignment) : horizontalGravity(alignment);
                String arrangement = props.getOrDefault("layout.arrangement", "start");
                gravity |= linear.getOrientation() == LinearLayout.HORIZONTAL ? horizontalGravity(arrangement) : verticalGravity(arrangement);
                linear.setGravity(gravity);
            }
            if (h.widget instanceof GridLayout grid) grid.setColumnCount(integer(n, "layout.columns", 2));
            bindClick(h);
            applyModifiers(h);
            if (type.equals("tooltip")) h.view.setTooltipText(props.get("message"));
            if (type.equals("divider")) h.widget.setBackgroundColor(BORDER);
            if (type.equals("skeleton")) h.widget.setBackground(background(BORDER,
                    "circle".equals(props.get("shape")) ? dp(999) : dp(6), 0, 0));
            if (Set.of("card", "alert", "snackbar", "dialog", "drawer").contains(type)
                    && !props.containsKey("style.background")) {
                h.view.setBackground(background(Color.WHITE, dp(8), BORDER, dp(1)));
            }
            if (h.dialog != null) { h.dialog.setTitle(props.getOrDefault("title", "")); if (h.view.isAttachedToWindow()) showDialog(h); }
        } finally { h.applying = false; }
        applyEnabled(h, h.parent == null || h.parent.enabled());
    }

    private void bindAction(Handle h) {
        String type = h.node.type();
        String event = switch (type) {
            case "form" -> "submit"; case "snackbar" -> "action"; case "alert" -> "dismiss";
            case "chip" -> "remove"; case "code-block" -> "copy"; default -> "change";
        };
        boolean shown = type.equals("accordion-item") || h.node.eventNames().contains(event);
        h.action.setVisibility(shown ? View.VISIBLE : View.GONE);
        h.action.setText(switch (type) {
            case "form" -> "Submit"; case "snackbar" -> h.node.props().getOrDefault("actionLabel", "Action");
            case "alert" -> "Dismiss"; case "chip" -> "Remove"; case "code-block" -> "Copy";
            default -> bool(h.node, "expanded", false) ? "Collapse" : "Expand";
        });
        h.action.setOnClickListener(v -> {
            if (type.equals("code-block") && h.enabled()) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Code", h.node.text()));
            }
            h.dispatch(event, type.equals("accordion-item") ? !bool(h.node, "expanded", false) : null);
        });
    }

    private void bindClick(Handle h) {
        if (h.node.type().equals("segment")) {
            h.widget.setOnClickListener(view -> {
                if (h.parent != null && h.enabled()) h.parent.dispatch("change", h.node.props().get("value"));
            });
            return;
        }
        if (h.widget instanceof CompoundButton || h.widget instanceof EditText || h.widget instanceof Spinner
                || h.node.type().equals("date-picker")) return;
        String event = null;
        for (String candidate : List.of("click", "select", "open", "navigate")) {
            if (h.node.eventNames().contains(candidate)) { event = candidate; break; }
        }
        final String chosen = event;
        h.widget.setOnClickListener(chosen == null ? null : view -> h.dispatch(chosen, null));
        h.widget.setClickable(chosen != null);
    }

    private void bindSpinner(Handle h, Spinner spinner, ViewNode previous) {
        List<String> options = h.node.children().stream().map(child -> child.props().get("value")).toList();
        List<String> before = previous == null ? List.of() : previous.children().stream().map(child -> child.props().get("value")).toList();
        if (!options.equals(before)) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        }
        spinner.setSelection(options.indexOf(h.node.props().get("selectedValue")), false);
        spinner.setContentDescription(h.node.props().get("label"));
    }

    private void bindImage(Handle h, ImageView image, ViewNode previous) {
        image.setScaleType(switch (h.node.props().getOrDefault("scale", "fit")) {
            case "crop" -> ImageView.ScaleType.CENTER_CROP; case "fill" -> ImageView.ScaleType.FIT_XY;
            case "none" -> ImageView.ScaleType.CENTER; default -> ImageView.ScaleType.FIT_CENTER;
        });
        String source = h.node.props().get("source");
        if (source != null && (previous == null || !source.equals(previous.props().get("source")))) {
            cancelImage(h); image.setImageDrawable(null); h.imageError = null;
            long ticket = ++h.imageGeneration;
            h.imageRequest = images.load(source, drawable -> {
                if (!h.disposed && h.imageGeneration == ticket) image.setImageDrawable(drawable);
            }, failure -> { if (!h.disposed && h.imageGeneration == ticket) h.imageError = failure; });
        }
    }

    @Override public void setChildren(Handle parent, List<Handle> children) {
        checkOpen();
        if (parent.node.type().equals("scroll") && children.size() > 1) throw new IllegalArgumentException("Scroll needs one child");
        View focused = parent.view.findFocus();
        for (Handle old : parent.children) {
            if (!children.contains(old)) {
                if (parent.target != null) parent.target.removeView(old.view);
                old.parent = null;
            }
        }
        parent.children = List.copyOf(children);
        for (Handle child : children) {
            if (child.parent != null && child.parent != parent) throw new IllegalStateException("Child is already attached");
            child.parent = parent;
        }
        if (parent.target != null) {
            int offset = parent.target.getChildCount();
            for (Handle child : children) if (child.view.getParent() == parent.target) offset--;
            for (int i = 0; i < children.size(); i++) {
                Handle child = children.get(i);
                if (parent.target.indexOfChild(child.view) != offset + i) {
                    if (child.view.getParent() == parent.target) parent.target.removeView(child.view);
                    parent.target.addView(child.view, offset + i);
                }
                if (parent.node.type().equals("segmented-control")) {
                    child.widget.setOnClickListener(v -> parent.dispatch("change", child.node.props().get("value")));
                }
            }
            layoutChildren(parent);
        }
        applyEnabled(parent, parent.parent == null || parent.parent.enabled());
        if (focused != null && focused.isAttachedToWindow()) focused.requestFocus();
    }

    private void layoutChildren(Handle parent) {
        if (parent.target == null) return;
        int index = 0;
        for (Handle child : parent.children) {
            if (child.node == null) continue;
            ViewNode n = child.node;
            int width = dimension(n.props().get("style.width"), defaultWidth(n));
            int height = dimension(n.props().get("style.height"), defaultHeight(n));
            ViewGroup.MarginLayoutParams params;
            if (parent.target instanceof LinearLayout linear) {
                float weight = (float) number(n, "layout.weight", 0);
                if (Set.of("navigation-bar", "tabs", "segmented-control", "table-row").contains(parent.node.type())) weight = 1;
                if (parent.node.type().equals("scaffold") && "content".equals(n.props().get("name"))) weight = 1;
                if (parent.node.type().equals("scaffold")) width = ViewGroup.LayoutParams.MATCH_PARENT;
                boolean horizontal = linear.getOrientation() == LinearLayout.HORIZONTAL;
                if (weight > 0) { if (horizontal) width = 0; else height = 0; }
                String alignment = parent.node.props().getOrDefault("layout.alignment", "start");
                if (alignment.equals("stretch")) { if (horizontal) height = ViewGroup.LayoutParams.MATCH_PARENT; else width = ViewGroup.LayoutParams.MATCH_PARENT; }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, weight);
                String self = n.props().get("layout.alignSelf");
                if (self != null) {
                    if (self.equals("stretch")) { if (horizontal) lp.height = ViewGroup.LayoutParams.MATCH_PARENT; else lp.width = ViewGroup.LayoutParams.MATCH_PARENT; }
                    else lp.gravity = horizontal ? verticalGravity(self) : horizontalGravity(self);
                }
                params = lp;
            } else if (parent.target instanceof GridLayout) {
                int columns = integer(parent.node, "layout.columns", 2);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = n.props().containsKey("style.width") ? width : 0; lp.height = height;
                lp.columnSpec = GridLayout.spec(index % columns, 1, 1f); lp.rowSpec = GridLayout.spec(index / columns);
                params = lp;
            } else {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
                lp.gravity = gravity(n.props().getOrDefault("layout.alignSelf",
                        parent.node.props().getOrDefault("layout.contentAlignment", "start")));
                params = lp;
            }
            int margin = dimension(n.props().get("style.margin"), 0);
            params.setMargins(margin, margin, margin, margin);
            int gap = dimension(parent.node.props().get("layout.gap"), 0);
            if (parent.target instanceof LinearLayout linear && index > 0) {
                if (linear.getOrientation() == LinearLayout.HORIZONTAL) params.setMarginStart(margin + gap);
                else params.topMargin += gap;
            } else if (parent.target instanceof GridLayout) {
                params.rightMargin += gap; params.bottomMargin += gap;
            }
            child.view.setLayoutParams(params);
            child.view.setVisibility(parent.node.type().equals("accordion-item") && !bool(parent.node, "expanded", false)
                    ? View.GONE : bool(n, "state.visible", true) ? View.VISIBLE : View.GONE);
            index++;
        }
        parent.target.requestLayout();
    }

    @Override public void setRoot(Handle next) {
        checkOpen();
        if (root == next) return;
        if (root != null) container.removeView(root.view);
        root = next;
        if (next != null) {
            applyRootSize(next);
            container.addView(next.view);
        }
    }

    private void applyRootSize(Handle h) {
        h.view.setLayoutParams(new ViewGroup.LayoutParams(dimension(h.node.props().get("style.width"), ViewGroup.LayoutParams.MATCH_PARENT),
                dimension(h.node.props().get("style.height"), ViewGroup.LayoutParams.MATCH_PARENT)));
    }

    @Override public void dispose(Handle h) {
        checkThread();
        if (h.disposed) return;
        h.disposed = true;
        if (h.restore != null) h.view.removeCallbacks(h.restore);
        cancelImage(h);
        if (h.adapter != null) h.adapter.close();
        if (h.picker != null) h.picker.dismiss();
        dismissDialog(h);
        h.node = null; h.children = List.of(); h.parent = null;
        h.view.removeAllViews(); live.remove(h);
    }

    @Override public void close() {
        checkThread();
        if (closed) return;
        if (!live.isEmpty()) throw new IllegalStateException("Close the mounted runtime/tree before its backend");
        closed = true;
        if (ownsImages && images instanceof AndroidImageLoader loader) loader.close();
    }

    private void applyModifiers(Handle h) {
        Map<String, String> props = h.node.props();
        BoundedLayout view = h.view;
        String padding = props.getOrDefault("style.padding", "0dp"); String[] parts = padding.split(" ");
        int vertical = dimension(parts[0], 0), horizontal = dimension(parts.length > 1 ? parts[1] : parts[0], 0);
        view.setPadding(horizontal, vertical, horizontal, vertical);
        view.setAlpha(h.node.property(StandardProps.OPACITY, 1.0).floatValue());
        view.setElevation(dimension(props.get("style.shadow"), 0));
        view.setTranslationZ(dp(integer(h.node, "layout.zIndex", 0)));
        view.setVisibility(bool(h.node, "state.visible", true) ? View.VISIBLE : View.GONE);
        view.setMinimumWidth(dimension(props.get("style.minWidth"), 0));
        view.setMinimumHeight(dimension(props.get("style.minHeight"), 0));
        view.maxWidth = dimension(props.get("style.maxWidth"), Integer.MAX_VALUE);
        view.maxHeight = dimension(props.get("style.maxHeight"), Integer.MAX_VALUE);
        if (view.maxWidth < view.getMinimumWidth() || view.maxHeight < view.getMinimumHeight()) throw new IllegalArgumentException("Minimum exceeds maximum");
        view.aspectRatio = h.node.property(StandardProps.ASPECT_RATIO, 0.0);
        String border = props.get("style.border");
        if (props.containsKey("style.background") || border != null || props.containsKey("style.cornerRadius")) {
            String[] stroke = border == null ? new String[0] : border.split(" ");
            view.setBackground(background(color(props.get("style.background"), Color.TRANSPARENT),
                    dimension(props.get("style.cornerRadius"), 0), stroke.length >= 3 ? color(stroke[2], BORDER) : BORDER,
                    stroke.length > 0 ? dimension(stroke[0], 0) : 0));
            view.setClipToOutline(dimension(props.get("style.cornerRadius"), 0) > 0);
        } else view.setBackground(null);
        h.widget.setContentDescription(props.getOrDefault("semantics.contentDescription", props.get("label")));
        view.setTag(props.get("semantics.testTag"));
        if (props.containsKey("interaction.focusable")) h.widget.setFocusable(bool(h.node, "interaction.focusable", false));
        if (h.widget instanceof TextView text && props.containsKey("style.foreground")) text.setTextColor(color(props.get("style.foreground"), INK));
        String role = props.get("semantics.role");
        h.widget.setAccessibilityDelegate(role == null ? null : new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                if (h.node == null) return;
                info.setClassName(switch (role) {
                    case "button", "tab", "menuitem", "link" -> Button.class.getName();
                    case "checkbox" -> CheckBox.class.getName(); case "switch" -> Switch.class.getName();
                    case "radio" -> RadioButton.class.getName();
                    case "slider" -> SeekBar.class.getName(); case "list" -> ListView.class.getName();
                    case "grid", "table" -> GridView.class.getName(); case "progressbar" -> ProgressBar.class.getName();
                    default -> host.getClass().getName();
                });
                info.setSelected(bool(h.node, "selected", false));
            }
        });
        view.requestLayout();
    }

    private void applyEnabled(Handle h, boolean inherited) {
        boolean enabled = inherited && h.node != null && bool(h.node, "state.enabled", true) && bool(h.node, "enabled", true);
        setEnabledRecursively(h.view, enabled);
        if (h.dialog != null) setEnabledRecursively(h.widget, enabled);
        if (h.previous != null) {
            h.previous.setEnabled(enabled && integer(h.node, "currentPage", 1) > 1);
            h.next.setEnabled(enabled && integer(h.node, "currentPage", 1) < integer(h.node, "pageCount", 1));
        }
        for (Handle child : h.children) applyEnabled(child, enabled);
    }

    private void setEnabledRecursively(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) setEnabledRecursively(group.getChildAt(i), enabled);
    }

    private void showDialog(Handle h) {
        if (h.disposed || !bool(h.node, "state.visible", true)) { dismissDialog(h); return; }
        if (!h.dialog.isShowing()) h.dialog.show();
        Window window = h.dialog.getWindow();
        if (window != null && h.node.type().equals("drawer")) {
            window.setGravity("end".equals(h.node.props().get("edge")) ? Gravity.END : Gravity.START);
            window.setLayout((int) (context.getResources().getDisplayMetrics().widthPixels * .8), ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void dismissDialog(Handle h) {
        if (h.dialog != null) {
            boolean applying = h.applying; h.applying = true;
            try { h.dialog.dismiss(); } finally { h.applying = applying; }
        }
    }

    private void cancelImage(Handle h) {
        h.imageGeneration++;
        if (h.imageRequest != null) {
            try { h.imageRequest.close(); } catch (Exception failure) { h.imageError = failure; }
            h.imageRequest = null;
        }
    }

    private TextView label() { TextView text = new TextView(context); text.setTextColor(INK); text.setTextSize(14); return text; }
    private Button button() { Button button = new Button(context); button.setAllCaps(false); button.setMinHeight(dp(48)); return button; }

    private String title(ViewNode n) {
        if (n.type().equals("code-block")) return n.text();
        return n.props().getOrDefault("title", n.props().getOrDefault("headline", n.props().getOrDefault("label", n.props().getOrDefault("message", ""))));
    }

    private String textValue(ViewNode n) {
        if (n.text() != null) return n.text();
        if (n.type().equals("avatar")) return n.props().getOrDefault("initials", "");
        if (Set.of("icon", "icon-button", "floating-action-button").contains(n.type())) return icon(n.props().get("icon"));
        if (n.type().equals("navigation-item")) return icon(n.props().get("icon")) + " " + n.props().getOrDefault("label", "");
        return n.props().getOrDefault("label", n.props().getOrDefault("value", ""));
    }

    private void styleText(TextView text, Map<String, String> props) {
        text.setTextSize((float) unit(props.getOrDefault("text.fontSize", "14sp")));
        text.setTextColor(color(props.get("text.color"), INK));
        int weight = (int) unit(props.getOrDefault("text.fontWeight", "400"));
        int style = weight >= 600 ? Typeface.BOLD : Typeface.NORMAL;
        if (Boolean.parseBoolean(props.get("text.italic"))) style |= Typeface.ITALIC;
        text.setTypeface(Typeface.create(props.getOrDefault("text.fontFamily", "sans-serif"), style));
        text.setGravity(gravity(props.getOrDefault("text.align", "start")));
        int lines = (int) unit(props.getOrDefault("text.maxLines", Integer.toString(Integer.MAX_VALUE)));
        text.setMaxLines(lines); text.setEllipsize(props.containsKey("text.maxLines") ? TextUtils.TruncateAt.END : null);
        text.setLineSpacing(0, (float) unit(props.getOrDefault("text.lineHeight", "1")));
        int flags = text.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG;
        if (Boolean.parseBoolean(props.get("text.underline"))) flags |= android.graphics.Paint.UNDERLINE_TEXT_FLAG;
        text.setPaintFlags(flags);
    }

    private String shape(ViewNode n) {
        return n.type() + ":" + n.props().get("orientation") + ":" + n.props().get("kind") + ":"
                + n.props().get("style") + ":" + n.props().get("interaction.scrollable");
    }

    private void validate(ViewNode n) {
        if (!TYPES.contains(n.type())) throw new UnsupportedOperationException("Unsupported Android node: " + n.type());
        for (String name : n.props().keySet()) {
            if ((name.startsWith("style.") || name.startsWith("layout.") || name.startsWith("interaction.")
                    || name.startsWith("semantics.") || name.startsWith("text.") || name.startsWith("state."))
                    && !MODIFIERS.contains(name)) throw new UnsupportedOperationException("Unsupported Android property: " + name);
        }
        if (Set.of("lazy-list", "lazy-grid").contains(n.type()) && n.lazyContent() == null) throw new IllegalArgumentException("Missing lazy content");
    }

    private SliderRange range(ViewNode n) { return new SliderRange(number(n, "minimum", 0), number(n, "maximum", 100), number(n, "step", 1)); }
    private boolean bool(ViewNode n, String name, boolean fallback) { return Boolean.parseBoolean(n.props().getOrDefault(name, Boolean.toString(fallback))); }
    private int integer(ViewNode n, String name, int fallback) { return Integer.parseInt(n.props().getOrDefault(name, Integer.toString(fallback))); }
    private double number(ViewNode n, String name, double fallback) { return unit(n.props().getOrDefault(name, Double.toString(fallback))); }
    private double unit(String value) {
        double parsed = Double.parseDouble(value.replace("dp", "").replace("sp", "").trim());
        if (!Double.isFinite(parsed)) throw new IllegalArgumentException("Nonfinite dimension or number");
        return parsed;
    }
    private int dimension(String value, int fallback) {
        if (value == null) return fallback;
        if (value.equals("100%")) return ViewGroup.LayoutParams.MATCH_PARENT;
        double parsed = unit(value);
        if (parsed < 0 || parsed > Integer.MAX_VALUE / context.getResources().getDisplayMetrics().density) throw new IllegalArgumentException("Invalid dimension");
        return dp((int) Math.round(parsed));
    }
    private int defaultWidth(ViewNode n) { return n.type().equals("divider") && !"vertical".equals(n.props().get("orientation")) ? -1
            : n.type().equals("divider") ? dimension(n.props().get("thickness"), dp(1)) : -2; }
    private int defaultHeight(ViewNode n) { return n.type().equals("divider") && "vertical".equals(n.props().get("orientation")) ? -1
            : n.type().equals("divider") ? dimension(n.props().get("thickness"), dp(1)) : -2; }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    private int color(String value, int fallback) { return value == null ? fallback : Color.parseColor(value); }
    private int horizontalGravity(String value) { return "center".equals(value) ? Gravity.CENTER_HORIZONTAL : "end".equals(value) ? Gravity.END : Gravity.START; }
    private int verticalGravity(String value) { return "center".equals(value) ? Gravity.CENTER_VERTICAL : "end".equals(value) ? Gravity.BOTTOM : Gravity.TOP; }
    private int gravity(String value) { return horizontalGravity(value) | verticalGravity(value); }
    private int variantColor(String variant) { return switch (variant) {
        case "warning" -> Color.rgb(233, 180, 76); case "danger" -> Color.rgb(231, 111, 81);
        case "info" -> Color.rgb(61, 111, 182); case "success" -> GREEN;
        case "ghost", "outlined" -> Color.TRANSPARENT; default -> INK;
    }; }
    private GradientDrawable background(int color, int radius, int strokeColor, int width) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(radius);
        if (width > 0) drawable.setStroke(width, strokeColor); return drawable;
    }
    private String icon(String name) { return switch (name == null ? "" : name) {
        case "sun" -> "\u2600"; case "home" -> "\u2302"; case "settings" -> "\u2699";
        case "add" -> "+"; case "chart" -> "\u25a5"; case "person" -> "\u25cf"; default -> "\u2022";
    }; }
    private long dateMillis(String value) { return LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
    private static int inputType(String value) { return switch (value) {
        case "email" -> InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
        case "password" -> InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        case "number" -> InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
        case "telephone" -> InputType.TYPE_CLASS_PHONE; case "url" -> InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
        default -> InputType.TYPE_CLASS_TEXT;
    }; }
    private static void checkThread() { if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Android backend requires the UI thread"); }
    private void checkOpen() { checkThread(); if (closed) throw new IllegalStateException("Backend is closed"); }

    public final class Handle {
        private ViewNode node;
        private final BoundedLayout view;
        private View widget;
        private ViewGroup target;
        private TextView title, detail;
        private Button action, previous, next;
        private Handle parent;
        private List<Handle> children = List.of();
        private boolean disposed, applying;
        private Dialog dialog;
        private DatePickerDialog picker;
        private LazyAdapter adapter;
        private AutoCloseable imageRequest;
        private Exception imageError;
        private long imageGeneration;
        private Runnable restore;
        private Handle(ViewNode node, BoundedLayout view) { this.node = node; this.view = view; }
        public View view() { checkThread(); return view; }
        public View widget() { checkThread(); return widget; }
        public List<Handle> children() { checkThread(); return children; }
        public Dialog dialog() { checkThread(); return dialog; }
        public Exception imageError() { checkThread(); return imageError; }
        private boolean enabled() { return !disposed && node != null && bool(node, "state.enabled", true)
                && bool(node, "enabled", true) && (parent == null || parent.enabled()); }
        private boolean dispatch(String event, Object value) { return !applying && enabled() && node.dispatch(event, value); }
    }

    private final class LazyAdapter extends BaseAdapter implements AutoCloseable {
        private final Handle owner;
        private final Set<Row> rows = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<String, Long> ids = new HashMap<>();
        private final Map<String, Integer> positions = new HashMap<>();
        LazyAdapter(Handle owner) { this.owner = owner; }
        @Override public int getCount() { return owner.node == null ? 0 : owner.node.lazyContent().count(); }
        @Override public Object getItem(int position) { return owner.node.lazyContent().keyAt(position); }
        @Override public boolean hasStableIds() { return true; }
        @Override public long getItemId(int position) {
            String key = owner.node.lazyContent().keyAt(position);
            Integer previous = positions.put(key, position);
            if (previous != null && previous != position) throw new IllegalArgumentException("Duplicate lazy key: " + key);
            return ids.computeIfAbsent(key, ignored -> (long) ids.size());
        }
        @Override public View getView(int position, View recycled, ViewGroup parent) {
            Row row;
            if (recycled != null && recycled.getTag() instanceof Row existing && rows.contains(existing)) row = existing;
            else { row = new Row(); rows.add(row); }
            ViewNode node = owner.node.lazyContent().nodeAt(position);
            row.tree.render(node);
            row.backend.root.parent = owner;
            row.backend.applyEnabled(row.backend.root, owner.enabled());
            return row.container;
        }
        @Override public void notifyDataSetChanged() { positions.clear(); super.notifyDataSetChanged(); }
        @Override public void close() {
            Throwable first = null;
            for (Row row : rows) {
                try { row.tree.close(); row.backend.close(); }
                catch (RuntimeException | Error failure) { if (first == null) first = failure; else first.addSuppressed(failure); }
            }
            rows.clear(); ids.clear(); positions.clear();
            if (first instanceof RuntimeException failure) throw failure;
            if (first instanceof Error failure) throw failure;
        }
        private final class Row {
            final FrameLayout container = new FrameLayout(context);
            final VelaAndroidBackend backend = new VelaAndroidBackend(context, container, images);
            final MountedTree<Handle> tree = new MountedTree<>(backend);
            Row() { container.setTag(this); }
        }
    }
}
