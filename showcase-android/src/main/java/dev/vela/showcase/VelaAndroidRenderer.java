package dev.vela.showcase;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.vela.core.ViewNode;

final class VelaAndroidRenderer {
    private static final int INK = Color.parseColor("#17211D");
    private static final int MUTED = Color.parseColor("#637069");
    private static final int BORDER = Color.parseColor("#DCE2DE");
    private static final int WHITE = Color.WHITE;
    private static final int EMERALD = Color.parseColor("#237A57");
    private static final int AMBER = Color.parseColor("#E9B44C");
    private static final int CORAL = Color.parseColor("#E76F51");
    private static final int BLUE = Color.parseColor("#3D6FB6");

    private final Context context;
    private final float density;

    VelaAndroidRenderer(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    View render(ViewNode node) {
        View view = switch (node.type()) {
            case "scaffold" -> scaffold(node);
            case "column" -> linear(node, LinearLayout.VERTICAL);
            case "row" -> linear(node, LinearLayout.HORIZONTAL);
            case "box", "surface" -> frame(node);
            case "grid" -> grid(node);
            case "scroll" -> scroll(node);
            case "slot", "tooltip" -> unwrap(node);
            case "spacer" -> new Space(context);
            case "text" -> text(node);
            case "button" -> button(node);
            case "icon-button", "floating-action-button" -> iconButton(node);
            case "card" -> card(node);
            case "chip" -> chip(node);
            case "badge" -> badge(node);
            case "avatar" -> avatar(node);
            case "divider" -> divider(node);
            case "checkbox" -> checkbox(node);
            case "switch" -> switchControl(node);
            case "slider" -> slider(node);
            case "progress" -> progress(node);
            case "alert" -> alert(node);
            case "top-app-bar" -> topAppBar(node);
            case "navigation-bar" -> navigationBar(node);
            case "navigation-item" -> navigationItem(node);
            case "list", "form", "accordion", "menu" -> linear(node, LinearLayout.VERTICAL);
            case "list-item" -> listItem(node);
            case "table" -> table(node);
            case "key-value-list" -> keyValueList(node);
            case "segmented-control" -> segmentedControl(node);
            case "image" -> image(node);
            case "empty-state", "snackbar", "dialog", "drawer", "accordion-item" -> genericPanel(node);
            default -> genericNode(node);
        };
        applyCommon(view, node);
        return view;
    }

    private View scaffold(ViewNode node) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color("#F4F6F2", Color.WHITE));

        for (ViewNode slot : node.children()) {
            String name = slot.props().get("name");
            View child = render(slot.children().get(0));
            if ("content".equals(name)) {
                root.addView(child, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1));
            } else {
                root.addView(child, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        return root;
    }

    private View linear(ViewNode node, int orientation) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(orientation);
        layout.setGravity(layoutGravity(node, orientation));
        int gap = dpValue(node.props().get("layout.gap"), 0);
        boolean spaceBetween = "space-between".equals(node.props().get("layout.arrangement"));

        for (int index = 0; index < node.children().size(); index++) {
            ViewNode childNode = node.children().get(index);
            View child = render(childNode);
            if (spaceBetween && index > 0) {
                Space space = new Space(context);
                LinearLayout.LayoutParams spaceParams = orientation == LinearLayout.HORIZONTAL
                        ? new LinearLayout.LayoutParams(0, 1, 1)
                        : new LinearLayout.LayoutParams(1, 0, 1);
                layout.addView(space, spaceParams);
            }
            float weight = floatValue(childNode.props().get("layout.weight"), 0);
            int width = layoutWidth(childNode, defaultWidth(childNode));
            int height = layoutHeight(childNode, defaultHeight(childNode));
            if (weight > 0 && orientation == LinearLayout.HORIZONTAL) {
                width = 0;
            } else if (weight > 0 && orientation == LinearLayout.VERTICAL) {
                height = 0;
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    width,
                    height,
                    weight);
            if (!spaceBetween && orientation == LinearLayout.VERTICAL && index > 0) {
                params.topMargin = gap;
            } else if (!spaceBetween && orientation == LinearLayout.HORIZONTAL && index > 0) {
                params.leftMargin = gap;
            }
            applyMargins(params, childNode);
            layout.addView(child, params);
        }
        return layout;
    }

    private View frame(ViewNode node) {
        FrameLayout frame = new FrameLayout(context);
        for (ViewNode childNode : node.children()) {
            View child = render(childNode);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    layoutWidth(childNode, ViewGroup.LayoutParams.WRAP_CONTENT),
                    layoutHeight(childNode, ViewGroup.LayoutParams.WRAP_CONTENT));
            params.gravity = alignmentGravity(node.props().get("layout.contentAlignment"));
            frame.addView(child, params);
        }
        return frame;
    }

    private View grid(ViewNode node) {
        GridLayout grid = new GridLayout(context);
        int columns = intValue(node.props().get("layout.columns"), 2);
        int gap = dpValue(node.props().get("layout.gap"), 0);
        grid.setColumnCount(columns);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);

        for (int index = 0; index < node.children().size(); index++) {
            View child = render(node.children().get(index));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(index % columns, 1, 1f);
            params.rowSpec = GridLayout.spec(index / columns);
            int halfGap = gap / 2;
            params.setMargins(
                    index % columns == 0 ? 0 : halfGap,
                    index >= columns ? halfGap : 0,
                    index % columns == columns - 1 ? 0 : halfGap,
                    index < node.children().size() - columns ? halfGap : 0);
            grid.addView(child, params);
        }
        return grid;
    }

    private View scroll(ViewNode node) {
        if (node.children().isEmpty()) {
            return new Space(context);
        }
        if ("horizontal".equals(node.props().get("orientation"))) {
            android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setFillViewport(true);
            scroll.addView(render(node.children().get(0)), new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return scroll;
        }
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.addView(render(node.children().get(0)), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private View unwrap(ViewNode node) {
        if (node.children().isEmpty()) {
            return new Space(context);
        }
        return render(node.children().get(0));
    }

    private View text(ViewNode node) {
        TextView text = baseText();
        text.setText(node.text() != null ? node.text() : node.props().getOrDefault("value", ""));
        applyTextStyle(text, node.props());
        return text;
    }

    private View button(ViewNode node) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(node.props().getOrDefault("label", "Action"));
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setMinHeight(dp(48));
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setStateListAnimator(null);
        styleAction(button, node.props().get("variant"));
        boolean enabled = booleanValue(node.props().get("enabled"), true);
        button.setEnabled(enabled);
        if (enabled && node.eventNames().contains("click")) {
            button.setOnClickListener(view -> node.dispatch("click"));
        }
        return button;
    }

    private View iconButton(ViewNode node) {
        TextView button = baseText();
        button.setText(iconGlyph(node.props().get("icon")));
        button.setTextSize(20);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(node.props().get("semantics.contentDescription"));
        button.setBackground(rounded(colorForVariant(node.props().get("variant")), 8, 0, 0));
        if (node.eventNames().contains("click")) {
            button.setOnClickListener(view -> node.dispatch("click"));
        }
        return button;
    }

    private View card(ViewNode node) {
        LinearLayout card = (LinearLayout) linear(node, LinearLayout.VERTICAL);
        int background = switch (node.props().getOrDefault("variant", "outlined")) {
            case "primary" -> INK;
            case "success" -> color("#DDEFE5", WHITE);
            case "warning" -> color("#FFF4E5", WHITE);
            case "info" -> color("#EAF2F8", WHITE);
            default -> WHITE;
        };
        card.setBackground(rounded(background, 8, BORDER, 1));
        if (!node.props().containsKey("style.padding")) {
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
        }
        return card;
    }

    private View chip(ViewNode node) {
        TextView chip = baseText();
        chip.setText(node.props().getOrDefault("label", ""));
        chip.setTextSize(11);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        int background = softColorForVariant(node.props().get("variant"));
        chip.setBackground(rounded(background, 50, 0, 0));
        chip.setTextColor(INK);
        if (node.eventNames().contains("click")) {
            chip.setOnClickListener(view -> node.dispatch("click"));
        }
        return chip;
    }

    private View badge(ViewNode node) {
        TextView badge = baseText();
        badge.setText(node.props().getOrDefault("label", ""));
        badge.setTextSize(10);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        badge.setGravity(Gravity.CENTER);
        badge.setSingleLine(true);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badge.setBackground(rounded(softColorForVariant(node.props().get("variant")), 50, 0, 0));
        return badge;
    }

    private View avatar(ViewNode node) {
        TextView avatar = baseText();
        avatar.setText(node.props().getOrDefault("initials", "•"));
        avatar.setTextSize(14);
        avatar.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        avatar.setTextColor(INK);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(rounded(AMBER, 100, 0, 0));
        return avatar;
    }

    private View divider(ViewNode node) {
        View divider = new View(context);
        divider.setBackgroundColor(BORDER);
        return divider;
    }

    private View checkbox(ViewNode node) {
        CheckBox checkbox = new CheckBox(context);
        checkbox.setText(node.props().getOrDefault("label", ""));
        checkbox.setTextColor(INK);
        checkbox.setTextSize(14);
        checkbox.setButtonTintList(tint(EMERALD));
        checkbox.setChecked(booleanValue(node.props().get("checked"), false));
        checkbox.setEnabled(booleanValue(node.props().get("enabled"), true));
        checkbox.setPadding(0, dp(2), 0, dp(2));
        checkbox.setOnCheckedChangeListener((button, checked) -> {
            if (button.isPressed()) {
                node.dispatch("change", checked);
            }
        });
        return checkbox;
    }

    @SuppressWarnings("deprecation")
    private View switchControl(ViewNode node) {
        Switch control = new Switch(context);
        control.setShowText(false);
        control.setContentDescription(node.props().getOrDefault("label", "Switch"));
        control.setChecked(booleanValue(node.props().get("checked"), false));
        control.setEnabled(booleanValue(node.props().get("enabled"), true));
        control.setThumbTintList(tint(EMERALD));
        control.setTrackTintList(new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { color("#80B89E", EMERALD), color("#C9CFCC", BORDER) }));
        control.setOnCheckedChangeListener((button, checked) -> {
            if (button.isPressed()) {
                node.dispatch("change", checked);
            }
        });
        return control;
    }

    private View slider(ViewNode node) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        SeekBar seekBar = new SeekBar(context);
        double minimum = doubleValue(node.props().get("minimum"), 0);
        double maximum = doubleValue(node.props().get("maximum"), 100);
        double current = doubleValue(node.props().get("value"), minimum);
        seekBar.setMax(1000);
        seekBar.setProgress((int) Math.round((current - minimum) / (maximum - minimum) * 1000));
        seekBar.setProgressTintList(ColorStateList.valueOf(AMBER));
        seekBar.setThumbTintList(ColorStateList.valueOf(AMBER));
        seekBar.setContentDescription(node.props().getOrDefault("label", "Slider"));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    node.dispatch("change", minimum + (maximum - minimum) * progress / 1000.0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        container.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }

    private View progress(ViewNode node) {
        boolean circular = "circular".equals(node.props().get("style"));
        ProgressBar progress = circular
                ? new ProgressBar(context)
                : new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        boolean indeterminate = booleanValue(node.props().get("indeterminate"), false);
        progress.setIndeterminate(indeterminate);
        progress.setProgressTintList(ColorStateList.valueOf(EMERALD));
        progress.setIndeterminateTintList(ColorStateList.valueOf(EMERALD));
        if (!indeterminate) {
            progress.setMax(1000);
            progress.setProgress((int) Math.round(doubleValue(node.props().get("progress"), 0) * 1000));
        }
        if (!circular) {
            progress.setMinimumHeight(dp(6));
        }
        return progress;
    }

    private View alert(ViewNode node) {
        LinearLayout alert = new LinearLayout(context);
        alert.setOrientation(LinearLayout.VERTICAL);
        alert.setPadding(dp(16), dp(14), dp(16), dp(14));
        alert.setBackground(rounded(softColorForVariant(node.props().get("variant")), 8, 0, 0));

        TextView title = baseText();
        title.setText(node.props().getOrDefault("title", ""));
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextSize(15);
        alert.addView(title);

        TextView message = baseText();
        message.setText(node.props().getOrDefault("message", ""));
        message.setTextColor(MUTED);
        message.setTextSize(13);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = dp(4);
        alert.addView(message, messageParams);

        if (node.eventNames().contains("dismiss")) {
            alert.setOnClickListener(view -> node.dispatch("dismiss"));
        }
        return alert;
    }

    private View topAppBar(ViewNode node) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(10), dp(20), dp(10));

        TextView title = baseText();
        title.setText(node.props().getOrDefault("title", ""));
        title.setTextSize(13);
        title.setLetterSpacing(0.16f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));

        for (ViewNode slot : node.children()) {
            if (!slot.children().isEmpty()) {
                View action = render(slot.children().get(0));
                bar.addView(action, new LinearLayout.LayoutParams(dp(40), dp(40)));
            }
        }
        return bar;
    }

    private View navigationBar(ViewNode node) {
        LinearLayout navigation = new LinearLayout(context);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(10), dp(8), dp(10), dp(8));
        navigation.setBackground(rounded(WHITE, 0, BORDER, 1));
        for (ViewNode item : node.children()) {
            navigation.addView(render(item), new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        return navigation;
    }

    private View navigationItem(ViewNode node) {
        boolean selected = booleanValue(node.props().get("selected"), false);
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(6), dp(5), dp(6), dp(5));
        if (selected) {
            item.setBackground(rounded(color("#E1EFE7", WHITE), 8, 0, 0));
        }

        TextView icon = baseText();
        icon.setText(iconGlyph(node.props().get("icon")));
        icon.setTextSize(18);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(selected ? EMERALD : MUTED);
        item.addView(icon, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));

        TextView label = baseText();
        label.setText(node.props().getOrDefault("label", ""));
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(Typeface.create("sans-serif-medium", selected ? Typeface.BOLD : Typeface.NORMAL));
        label.setTextColor(selected ? EMERALD : MUTED);
        item.addView(label);
        item.setOnClickListener(view -> node.dispatch("select"));
        return item;
    }

    private View listItem(ViewNode node) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView headline = baseText();
        headline.setText(node.props().getOrDefault("headline", ""));
        headline.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        headline.setTextSize(15);
        copy.addView(headline);
        String supporting = node.props().get("supportingText");
        if (supporting != null) {
            TextView detail = baseText();
            detail.setText(supporting);
            detail.setTextColor(MUTED);
            detail.setTextSize(12);
            copy.addView(detail);
        }
        item.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        for (ViewNode slot : node.children()) {
            if (!slot.children().isEmpty()) {
                item.addView(render(slot.children().get(0)));
            }
        }
        if (node.eventNames().contains("click")) {
            item.setOnClickListener(view -> node.dispatch("click"));
        }
        return item;
    }

    private View table(ViewNode node) {
        LinearLayout table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        for (ViewNode rowNode : node.children()) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            boolean header = booleanValue(rowNode.props().get("header"), false);
            if (header) {
                row.setBackgroundColor(color("#EEF1EF", Color.TRANSPARENT));
            }
            for (ViewNode cell : rowNode.children()) {
                TextView text = baseText();
                text.setText(cell.props().getOrDefault("value", ""));
                text.setTextSize(header ? 11 : 13);
                text.setTextColor(header ? MUTED : INK);
                text.setTypeface(Typeface.create("sans-serif-medium", header ? Typeface.BOLD : Typeface.NORMAL));
                text.setPadding(dp(8), dp(10), dp(8), dp(10));
                row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            }
            table.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return table;
    }

    private View keyValueList(ViewNode node) {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        for (ViewNode entry : node.children()) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(8), 0, dp(8));
            TextView key = baseText();
            key.setText(entry.props().getOrDefault("key", ""));
            key.setTextColor(MUTED);
            key.setTextSize(13);
            TextView value = baseText();
            value.setText(entry.props().getOrDefault("value", ""));
            value.setGravity(Gravity.END);
            value.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            value.setTextSize(13);
            row.addView(key, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            list.addView(row);
        }
        return list;
    }

    private View segmentedControl(ViewNode node) {
        LinearLayout control = new LinearLayout(context);
        control.setOrientation(LinearLayout.HORIZONTAL);
        control.setPadding(dp(3), dp(3), dp(3), dp(3));
        control.setBackground(rounded(color("#EEF1EF", WHITE), 8, 0, 0));
        for (ViewNode segment : node.children()) {
            String value = segment.props().getOrDefault("value", "");
            boolean selected = booleanValue(segment.props().get("selected"), false);
            TextView option = baseText();
            option.setText(value);
            option.setTextSize(12);
            option.setGravity(Gravity.CENTER);
            option.setTypeface(Typeface.create("sans-serif-medium", selected ? Typeface.BOLD : Typeface.NORMAL));
            option.setTextColor(selected ? INK : MUTED);
            if (selected) {
                option.setBackground(rounded(WHITE, 6, BORDER, 1));
                option.setElevation(dp(1));
            }
            option.setOnClickListener(view -> node.dispatch("change", value));
            control.addView(option, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        return control;
    }

    private View image(ViewNode node) {
        ImageView image = new ImageView(context);
        image.setContentDescription(node.props().get("semantics.contentDescription"));
        image.setBackground(rounded(color("#E6ECE8", WHITE), 8, 0, 0));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return image;
    }

    private View genericPanel(ViewNode node) {
        LinearLayout panel = (LinearLayout) genericNode(node);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(rounded(WHITE, 8, BORDER, 1));
        return panel;
    }

    private View genericNode(ViewNode node) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        String title = firstNonBlank(
                node.props().get("title"),
                node.props().get("label"),
                node.props().get("message"));
        if (title != null) {
            TextView text = baseText();
            text.setText(title);
            text.setTextSize(14);
            container.addView(text);
        }
        for (ViewNode child : node.children()) {
            container.addView(render(child));
        }
        return container;
    }

    private TextView baseText() {
        TextView text = new TextView(context);
        text.setTextColor(INK);
        text.setFontFeatureSettings("kern");
        text.setIncludeFontPadding(false);
        return text;
    }

    private void applyTextStyle(TextView text, Map<String, String> props) {
        text.setTextSize(unitValue(props.get("text.fontSize"), 14));
        text.setTextColor(color(props.get("text.color"), INK));
        int weight = intValue(props.get("text.fontWeight"), 400);
        boolean italic = booleanValue(props.get("text.italic"), false);
        int style = weight >= 600 ? Typeface.BOLD : Typeface.NORMAL;
        if (italic) {
            style |= Typeface.ITALIC;
        }
        String family = props.getOrDefault("text.fontFamily", weight >= 600 ? "sans-serif-medium" : "sans-serif");
        text.setTypeface(Typeface.create(family, style));
        text.setGravity(alignmentGravity(props.get("text.align")));
        text.setMaxLines(intValue(props.get("text.maxLines"), Integer.MAX_VALUE));
        text.setEllipsize(props.containsKey("text.maxLines") ? TextUtils.TruncateAt.END : null);
        double lineHeight = doubleValue(props.get("text.lineHeight"), 1);
        if (lineHeight != 1) {
            text.setLineSpacing(0, (float) lineHeight);
        }
        if (booleanValue(props.get("text.underline"), false)) {
            text.setPaintFlags(text.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        }
    }

    private void applyCommon(View view, ViewNode node) {
        Map<String, String> props = node.props();
        int verticalPadding;
        int horizontalPadding;
        String padding = props.get("style.padding");
        if (padding != null && padding.contains(" ")) {
            String[] values = padding.split(" ");
            verticalPadding = dpValue(values[0], 0);
            horizontalPadding = dpValue(values[1], 0);
        } else {
            verticalPadding = dpValue(padding, -1);
            horizontalPadding = verticalPadding;
        }
        if (verticalPadding >= 0) {
            view.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        }

        if (props.containsKey("style.background")) {
            int background = color(props.get("style.background"), Color.TRANSPARENT);
            int radius = Math.round(unitValue(props.get("style.cornerRadius"), 0));
            Border border = border(props.get("style.border"));
            view.setBackground(rounded(background, radius, border.color(), border.widthDp()));
        }
        if (view instanceof TextView text && props.containsKey("style.foreground")) {
            text.setTextColor(color(props.get("style.foreground"), INK));
        }
        if (props.containsKey("style.opacity")) {
            view.setAlpha(floatValue(props.get("style.opacity"), 1));
        }
        if (props.containsKey("style.shadow")) {
            view.setElevation(dp(Math.round(unitValue(props.get("style.shadow"), 0))));
        }
        view.setVisibility(booleanValue(props.get("state.visible"), true) ? View.VISIBLE : View.GONE);
        view.setEnabled(booleanValue(props.get("state.enabled"), view.isEnabled()));
        if (props.containsKey("semantics.contentDescription")) {
            view.setContentDescription(props.get("semantics.contentDescription"));
        }
        if (props.containsKey("semantics.testTag")) {
            view.setTag(props.get("semantics.testTag"));
        }
        if (props.containsKey("style.minWidth")) {
            view.setMinimumWidth(dpValue(props.get("style.minWidth"), 0));
        }
        if (props.containsKey("style.minHeight")) {
            view.setMinimumHeight(dpValue(props.get("style.minHeight"), 0));
        }
    }

    private int layoutGravity(ViewNode node, int orientation) {
        String alignment = node.props().get("layout.alignment");
        String arrangement = node.props().get("layout.arrangement");
        int gravity = 0;
        if (orientation == LinearLayout.VERTICAL) {
            gravity |= horizontalGravity(alignment);
            gravity |= verticalGravity(arrangement);
        } else {
            gravity |= verticalGravity(alignment);
            gravity |= horizontalGravity(arrangement);
        }
        return gravity == 0 ? Gravity.START | Gravity.TOP : gravity;
    }

    private int horizontalGravity(String value) {
        if ("center".equals(value)) {
            return Gravity.CENTER_HORIZONTAL;
        }
        if ("end".equals(value)) {
            return Gravity.END;
        }
        return Gravity.START;
    }

    private int verticalGravity(String value) {
        if ("center".equals(value)) {
            return Gravity.CENTER_VERTICAL;
        }
        if ("end".equals(value)) {
            return Gravity.BOTTOM;
        }
        return Gravity.TOP;
    }

    private int alignmentGravity(String value) {
        return switch (value == null ? "start" : value) {
            case "center" -> Gravity.CENTER;
            case "end" -> Gravity.END | Gravity.CENTER_VERTICAL;
            default -> Gravity.START | Gravity.CENTER_VERTICAL;
        };
    }

    private int layoutWidth(ViewNode node, int fallback) {
        String value = node.props().get("style.width");
        if ("100%".equals(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        return value == null ? fallback : dpValue(value, fallback);
    }

    private int defaultWidth(ViewNode node) {
        if ("divider".equals(node.type()) && "vertical".equals(node.props().get("orientation"))) {
            return dpValue(node.props().get("thickness"), dp(1));
        }
        if ("divider".equals(node.type())) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private int layoutHeight(ViewNode node, int fallback) {
        String value = node.props().get("style.height");
        if ("100%".equals(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        return value == null ? fallback : dpValue(value, fallback);
    }

    private int defaultHeight(ViewNode node) {
        if ("divider".equals(node.type()) && !"vertical".equals(node.props().get("orientation"))) {
            return dpValue(node.props().get("thickness"), dp(1));
        }
        if ("divider".equals(node.type())) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private void applyMargins(ViewGroup.MarginLayoutParams params, ViewNode node) {
        int margin = dpValue(node.props().get("style.margin"), 0);
        if (margin > 0) {
            params.setMargins(margin, margin, margin, margin);
        }
    }

    private void styleAction(Button button, String variant) {
        int background = colorForVariant(variant);
        int foreground = "warning".equals(variant) || "ghost".equals(variant) ? INK : WHITE;
        if ("ghost".equals(variant)) {
            background = Color.TRANSPARENT;
        }
        button.setTextColor(foreground);
        button.setBackground(rounded(background, 8, "ghost".equals(variant) ? BORDER : 0, 1));
    }

    private int colorForVariant(String variant) {
        return switch (variant == null ? "primary" : variant) {
            case "warning" -> AMBER;
            case "danger" -> CORAL;
            case "info" -> BLUE;
            case "success" -> EMERALD;
            case "secondary" -> color("#54645C", INK);
            case "ghost", "outlined" -> WHITE;
            default -> INK;
        };
    }

    private int softColorForVariant(String variant) {
        return switch (variant == null ? "primary" : variant) {
            case "warning" -> color("#FBE8B7", AMBER);
            case "danger" -> color("#F9D9D1", CORAL);
            case "info" -> color("#DCE9F8", BLUE);
            case "success" -> color("#D7EBDD", EMERALD);
            case "ghost", "outlined" -> color("#EEF1EF", WHITE);
            default -> color("#E1EFE7", WHITE);
        };
    }

    private GradientDrawable rounded(int background, int radiusDp, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(background);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0 && strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private ColorStateList tint(int checkedColor) {
        return new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { checkedColor, MUTED });
    }

    private Border border(String value) {
        if (value == null || value.isBlank()) {
            return new Border(0, 0);
        }
        String[] parts = value.split(" ");
        return new Border(
                Math.round(unitValue(parts[0], 1)),
                parts.length > 2 ? color(parts[2], BORDER) : BORDER);
    }

    private int dpValue(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.replace("dp", "").trim();
        try {
            return dp((int) Math.round(Double.parseDouble(normalized)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private float unitValue(String value, float fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.replace("sp", "").replace("dp", "").trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private int intValue(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private float floatValue(String value, float fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double doubleValue(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean booleanValue(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private int color(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private String iconGlyph(String name) {
        return switch (name == null ? "" : name.toLowerCase(Locale.ROOT)) {
            case "sun" -> "☀";
            case "chart" -> "▥";
            case "person" -> "●";
            case "home" -> "⌂";
            case "settings" -> "⚙";
            case "add" -> "+";
            default -> "•";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private int dp(int value) {
        return Math.round(value * density);
    }

    private record Border(int widthDp, int color) {
    }
}
