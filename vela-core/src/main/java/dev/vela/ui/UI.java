package dev.vela.ui;

import java.util.*;
import java.util.function.*;
import java.time.LocalDate;
import dev.vela.core.*;
import dev.vela.components.*;

/** Concise Java facade. All nodes use the existing validated component factories. */
public final class UI {
    private UI() { }
    public static ViewNode accordionItem(String title, boolean expanded, ViewNode content, Consumer<Boolean> onExpandedChange, Modifier modifier) { return VelaAccordion.item(title, expanded, content, onExpandedChange, modifier); }
    public static ViewNode accordionGroup(Modifier modifier, ViewNode... items) { return VelaAccordion.group(modifier, items); }
    public static ViewNode alert(String title, String message, ComponentVariant variant, Runnable onDismiss, Modifier modifier) { return VelaAlert.of(title, message, variant, onDismiss, modifier); }
    public static ViewNode avatarImage(String source, String contentDescription, int sizeDp, Modifier modifier) { return VelaAvatar.image(source, contentDescription, sizeDp, modifier); }
    public static ViewNode avatarInitials(String initials, int sizeDp, Modifier modifier) { return VelaAvatar.initials(initials, sizeDp, modifier); }
    public static ViewNode badge(String label) { return VelaBadge.of(label); }
    public static ViewNode badge(String label, ComponentVariant variant, Modifier modifier) { return VelaBadge.of(label, variant, modifier); }
    public static ViewNode box(ViewNode... children) { return VelaBox.of(children); }
    public static ViewNode box(Alignment contentAlignment, Modifier modifier, ViewNode... children) { return VelaBox.of(contentAlignment, modifier, children); }
    public static ViewNode breadcrumbs(List<String> labels, IntConsumer onNavigate, Modifier modifier) { return VelaBreadcrumbs.of(labels, onNavigate, modifier); }
    public static ViewNode button(String label) { return VelaButton.of(label); }
    public static ViewNode button(String label, Runnable onClick) { return VelaButton.of(label, onClick); }
    public static ViewNode button(String label, ComponentVariant variant, boolean enabled, Runnable onClick, Modifier modifier) { return VelaButton.of(label, variant, enabled, onClick, modifier); }
    public static ViewNode card(ViewNode... children) { return VelaCard.of(children); }
    public static ViewNode card(ComponentVariant variant, Modifier modifier, ViewNode... children) { return VelaCard.of(variant, modifier, children); }
    public static ViewNode checkbox(String label, boolean checked, boolean enabled, Consumer<Boolean> onCheckedChange, Modifier modifier) { return VelaCheckbox.of(label, checked, enabled, onCheckedChange, modifier); }
    public static ViewNode chip(String label, ComponentVariant variant, Runnable onClick, Modifier modifier) { return VelaChip.of(label, variant, onClick, modifier); }
    public static ViewNode chipRemovable(String label, ComponentVariant variant, Runnable onClick, Runnable onRemove, Modifier modifier) { return VelaChip.removable(label, variant, onClick, onRemove, modifier); }
    public static ViewNode codeBlock(String code, String language, Runnable onCopy, Modifier modifier) { return VelaCodeBlock.of(code, language, onCopy, modifier); }
    public static ViewNode column(ViewNode... children) { return VelaColumn.of(children); }
    public static ViewNode column(Modifier modifier, ViewNode... children) { return VelaColumn.of(modifier, children); }
    public static ViewNode column(Arrangement arrangement, Alignment alignment, int gapDp, Modifier modifier, ViewNode... children) { return VelaColumn.of(arrangement, alignment, gapDp, modifier, children); }
    public static ViewNode datePicker(String label, LocalDate value, LocalDate minimum, LocalDate maximum, Consumer<LocalDate> onDateChange, Modifier modifier) { return VelaDatePicker.of(label, value, minimum, maximum, onDateChange, modifier); }
    public static ViewNode dialog(String title, ViewNode content, ViewNode actions, Runnable onDismiss, Modifier modifier) { return VelaDialog.of(title, content, actions, onDismiss, modifier); }
    public static ViewNode dividerHorizontal() { return VelaDivider.horizontal(); }
    public static ViewNode divider(Orientation orientation, int thicknessDp, Modifier modifier) { return VelaDivider.of(orientation, thicknessDp, modifier); }
    public static ViewNode drawer(VelaDrawer.Edge edge, ViewNode content, Runnable onDismiss, Modifier modifier) { return VelaDrawer.of(edge, content, onDismiss, modifier); }
    public static ViewNode emptyState(String title, String message, ViewNode illustration, ViewNode action, Modifier modifier) { return VelaEmptyState.of(title, message, illustration, action, modifier); }
    public static ViewNode floatingActionButton(String icon, String contentDescription, ComponentVariant variant, Runnable onClick, Modifier modifier) { return VelaFloatingActionButton.of(icon, contentDescription, variant, onClick, modifier); }
    public static ViewNode form(String name, Runnable onSubmit, Modifier modifier, ViewNode... fields) { return VelaForm.of(name, onSubmit, modifier, fields); }
    public static ViewNode gridColumns(int columns, int gapDp, Modifier modifier, ViewNode... children) { return VelaGrid.columns(columns, gapDp, modifier, children); }
    public static ViewNode icon(String name, String contentDescription) { return VelaIcon.of(name, contentDescription); }
    public static ViewNode icon(String name, String contentDescription, Modifier modifier) { return VelaIcon.of(name, contentDescription, modifier); }
    public static ViewNode iconButton(String icon, String contentDescription, ComponentVariant variant, boolean enabled, Runnable onClick, Modifier modifier) { return VelaIconButton.of(icon, contentDescription, variant, enabled, onClick, modifier); }
    public static ViewNode image(String source, String contentDescription, Modifier modifier) { return VelaImage.of(source, contentDescription, modifier); }
    public static ViewNode image(String source, String contentDescription, VelaImage.Scale scale, Modifier modifier) { return VelaImage.of(source, contentDescription, scale, modifier); }
    public static ViewNode keyValue(Map<String, String> entries, Modifier modifier) { return VelaKeyValue.of(entries, modifier); }
    public static ViewNode lazyGridColumns(int columns, int count, IntFunction<String> key, IntFunction<ViewNode> item, Modifier modifier) { return VelaLazyGrid.columns(columns, count, key, item, modifier); }
    public static ViewNode lazyList(int count, IntFunction<String> key, IntFunction<ViewNode> item, Modifier modifier) { return VelaLazyList.of(count, key, item, modifier); }
    public static ViewNode link(String label, String destination, Runnable onOpen, Modifier modifier) { return VelaLink.of(label, destination, onOpen, modifier); }
    public static ViewNode list(boolean divided, Modifier modifier, ViewNode... items) { return VelaList.of(divided, modifier, items); }
    public static ViewNode listItem(String headline, String supportingText, ViewNode leading, ViewNode trailing, Runnable onClick, Modifier modifier) { return VelaListItem.of(headline, supportingText, leading, trailing, onClick, modifier); }
    public static ViewNode menu(Modifier modifier, ViewNode... items) { return VelaMenu.of(modifier, items); }
    public static ViewNode menuItem(String label, String icon, boolean enabled, Runnable onSelect, Modifier modifier) { return VelaMenu.item(label, icon, enabled, onSelect, modifier); }
    public static ViewNode navigationBar(Modifier modifier, ViewNode... items) { return VelaNavigationBar.of(modifier, items); }
    public static ViewNode navigationBarItem(String label, String icon, boolean selected, Runnable onSelect, Modifier modifier) { return VelaNavigationBar.item(label, icon, selected, onSelect, modifier); }
    public static ViewNode pagination(int currentPage, int pageCount, IntConsumer onPageChange, Modifier modifier) { return VelaPagination.of(currentPage, pageCount, onPageChange, modifier); }
    public static ViewNode progressIndicatorDeterminate(double progress, VelaProgressIndicator.Style style, Modifier modifier) { return VelaProgressIndicator.determinate(progress, style, modifier); }
    public static ViewNode progressIndicatorIndeterminate(VelaProgressIndicator.Style style, Modifier modifier) { return VelaProgressIndicator.indeterminate(style, modifier); }
    public static ViewNode radioButton(String label, String value, boolean selected, boolean enabled, Runnable onSelect, Modifier modifier) { return VelaRadioButton.of(label, value, selected, enabled, onSelect, modifier); }
    public static ViewNode row(ViewNode... children) { return VelaRow.of(children); }
    public static ViewNode row(Modifier modifier, ViewNode... children) { return VelaRow.of(modifier, children); }
    public static ViewNode row(Arrangement arrangement, Alignment alignment, int gapDp, Modifier modifier, ViewNode... children) { return VelaRow.of(arrangement, alignment, gapDp, modifier, children); }
    public static ViewNode scaffold(ViewNode topBar, ViewNode content, ViewNode bottomBar, Modifier modifier) { return VelaScaffold.of(topBar, content, bottomBar, modifier); }
    public static ViewNode scroll(Orientation orientation, Modifier modifier, ViewNode child) { return VelaScroll.of(orientation, modifier, child); }
    public static ViewNode segmentedControl(List<String> options, String selectedValue, Consumer<String> onSelectionChange, Modifier modifier) { return VelaSegmentedControl.of(options, selectedValue, onSelectionChange, modifier); }
    public static ViewNode select(String label, String selectedValue, List<String> options, Consumer<String> onSelectionChange, Modifier modifier) { return VelaSelect.of(label, selectedValue, options, onSelectionChange, modifier); }
    public static ViewNode skeleton(VelaSkeleton.Shape shape, Modifier modifier) { return VelaSkeleton.of(shape, modifier); }
    public static ViewNode slider(String label, double value, double minimum, double maximum, double step, DoubleConsumer onValueChange, Modifier modifier) { return VelaSlider.of(label, value, minimum, maximum, step, onValueChange, modifier); }
    public static ViewNode snackbar(String message, String actionLabel, Runnable onAction, Modifier modifier) { return VelaSnackbar.of(message, actionLabel, onAction, modifier); }
    public static ViewNode spacerSize(int widthDp, int heightDp) { return VelaSpacer.size(widthDp, heightDp); }
    public static ViewNode spacerHorizontal(int widthDp) { return VelaSpacer.horizontal(widthDp); }
    public static ViewNode spacerVertical(int heightDp) { return VelaSpacer.vertical(heightDp); }
    public static ViewNode surface(Modifier modifier, ViewNode... children) { return VelaSurface.of(modifier, children); }
    public static ViewNode toggle(String label, boolean checked, boolean enabled, Consumer<Boolean> onCheckedChange, Modifier modifier) { return VelaSwitch.of(label, checked, enabled, onCheckedChange, modifier); }
    public static ViewNode table(List<String> headers, List<List<String>> rows, Modifier modifier) { return VelaTable.of(headers, rows, modifier); }
    public static ViewNode tabs(Modifier modifier, ViewNode... tabs) { return VelaTabs.of(modifier, tabs); }
    public static ViewNode tabsTab(String label, boolean selected, Runnable onSelect, Modifier modifier) { return VelaTabs.tab(label, selected, onSelect, modifier); }
    public static ViewNode text(String value) { return VelaText.of(value); }
    public static ViewNode text(String value, Modifier modifier) { return VelaText.of(value, modifier); }
    public static ViewNode text(String value, TextStyle style, Modifier modifier) { return VelaText.of(value, style, modifier); }
    public static ViewNode textField(String label, String value, String placeholder, VelaTextField.InputType inputType, boolean enabled, Consumer<String> onValueChange, Modifier modifier) { return VelaTextField.of(label, value, placeholder, inputType, enabled, onValueChange, modifier); }
    public static ViewNode textFieldMultiline(String label, String value, String placeholder, int minimumLines, Consumer<String> onValueChange, Modifier modifier) { return VelaTextField.multiline(label, value, placeholder, minimumLines, onValueChange, modifier); }
    public static ViewNode tooltip(String message, ViewNode anchor, Modifier modifier) { return VelaTooltip.of(message, anchor, modifier); }
    public static ViewNode topAppBar(String title, ViewNode navigation, Modifier modifier, ViewNode... actions) { return VelaTopAppBar.of(title, navigation, modifier, actions); }

    public static Modifier padding(int dp) { return Modifier.none().padding(dp); }
    public static Modifier background(String color) { return Modifier.none().background(color); }
    public static Modifier gap(int dp) {
        if (dp < 0) throw new IllegalArgumentException("gap must not be negative");
        return Modifier.none().property("layout.gap", dp + "dp");
    }
    public static ViewNode button(String label, Runnable click, ButtonOptions options) {
        return VelaButton.of(label, options.variant(), options.enabled(), click, options.modifier());
    }
    public static ViewNode field(String label, MutableState<String> state) { return field(label, state, FieldOptions.defaults()); }
    public static ViewNode field(String label, MutableState<String> state, FieldOptions options) {
        return VelaTextField.of(label, state.get(), options.placeholder(), options.inputType(), options.enabled(), state::set, options.modifier());
    }
    public static ViewNode toggle(String label, MutableState<Boolean> state) {
        return VelaSwitch.of(label, state.get(), true, state::set, Modifier.none());
    }
    public static ViewNode checkbox(String label, MutableState<Boolean> state) {
        return VelaCheckbox.of(label, state.get(), true, state::set, Modifier.none());
    }
    public static ViewNode slider(String label, MutableState<Double> state) { return slider(label, state, SliderOptions.defaults()); }
    public static ViewNode slider(String label, MutableState<Double> state, SliderOptions options) {
        return VelaSlider.of(label, state.get(), options.minimum(), options.maximum(), options.step(), state::set, options.modifier());
    }
}
