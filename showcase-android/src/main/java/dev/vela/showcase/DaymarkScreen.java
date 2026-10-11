package dev.vela.showcase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import dev.vela.annotation.State;
import dev.vela.components.VelaAlert;
import dev.vela.components.VelaAvatar;
import dev.vela.components.VelaBadge;
import dev.vela.components.VelaButton;
import dev.vela.components.VelaCard;
import dev.vela.components.VelaCheckbox;
import dev.vela.components.VelaChip;
import dev.vela.components.VelaColumn;
import dev.vela.components.VelaDivider;
import dev.vela.components.VelaGrid;
import dev.vela.components.VelaKeyValue;
import dev.vela.components.VelaList;
import dev.vela.components.VelaListItem;
import dev.vela.components.VelaNavigationBar;
import dev.vela.components.VelaProgressIndicator;
import dev.vela.components.VelaRow;
import dev.vela.components.VelaScaffold;
import dev.vela.components.VelaScroll;
import dev.vela.components.VelaSegmentedControl;
import dev.vela.components.VelaSlider;
import dev.vela.components.VelaSpacer;
import dev.vela.components.VelaSwitch;
import dev.vela.components.VelaTable;
import dev.vela.components.VelaText;
import dev.vela.components.VelaTopAppBar;
import dev.vela.core.VelaComponent;
import dev.vela.core.ViewNode;
import dev.vela.ui.Alignment;
import dev.vela.ui.Arrangement;
import dev.vela.ui.ComponentVariant;
import dev.vela.ui.Modifier;
import dev.vela.ui.Orientation;
import dev.vela.ui.TextStyle;

public final class DaymarkScreen extends VelaComponent {
    private static final TextStyle EYEBROW = TextStyle.defaultStyle()
            .fontSize(12)
            .fontWeight(700)
            .color("#637069");
    private static final TextStyle DISPLAY = TextStyle.defaultStyle()
            .fontSize(34)
            .fontWeight(700)
            .color("#17211D")
            .lineHeight(1.08);
    private static final TextStyle SECTION = TextStyle.defaultStyle()
            .fontSize(20)
            .fontWeight(700)
            .color("#17211D");
    private static final TextStyle CARD_TITLE = TextStyle.defaultStyle()
            .fontSize(17)
            .fontWeight(700)
            .color("#17211D");
    private static final TextStyle BODY = TextStyle.defaultStyle()
            .fontSize(14)
            .color("#637069")
            .lineHeight(1.35);

    private final Runnable invalidate;

    @State
    private int selectedPage;
    @State
    private int waterGlasses = 5;
    @State
    private int focusSessions = 2;
    @State
    private boolean focusActive;
    @State
    private boolean quietNotifications = true;
    @State
    private double energy = 72;
    @State
    private boolean planTaskDone = true;
    @State
    private boolean designTaskDone;
    @State
    private boolean walkTaskDone;
    @State
    private String weekMode = "Balanced";

    public DaymarkScreen(Runnable invalidate) {
        this.invalidate = Objects.requireNonNull(invalidate, "invalidate");
    }

    @Override
    protected void onStateChanged() {
        invalidate.run();
    }

    @Override
    public ViewNode render() {
        return VelaScaffold.of(
                appBar(),
                VelaScroll.of(Orientation.VERTICAL, Modifier.none().fillMaxWidth(), pageContent()),
                navigation(),
                Modifier.none().background("#F4F6F2"));
    }

    private ViewNode appBar() {
        return VelaTopAppBar.of(
                "DAYMARK",
                null,
                Modifier.none().fillMaxWidth().background("#F4F6F2").padding(16),
                VelaAvatar.initials("AR", 38, Modifier.none()));
    }

    private ViewNode navigation() {
        return VelaNavigationBar.of(
                Modifier.none().fillMaxWidth().background("#FFFFFF").padding(8),
                VelaNavigationBar.item("Today", "sun", selectedPage == 0, () -> selectPage(0), Modifier.none()),
                VelaNavigationBar.item(
                        "Insights", "chart", selectedPage == 1, () -> selectPage(1), Modifier.none()),
                VelaNavigationBar.item(
                        "Profile", "person", selectedPage == 2, () -> selectPage(2), Modifier.none()));
    }

    private ViewNode pageContent() {
        return switch (selectedPage) {
            case 1 -> insightsPage();
            case 2 -> profilePage();
            default -> todayPage();
        };
    }

    private ViewNode todayPage() {
        ViewNode focusStatus = focusActive
                ? VelaAlert.of(
                        "Focus is running",
                        "A quiet 25-minute block is in progress.",
                        ComponentVariant.SUCCESS,
                        this::stopFocus,
                        Modifier.none().fillMaxWidth())
                : VelaSpacer.vertical(0);

        return VelaColumn.of(
                Arrangement.START,
                Alignment.STRETCH,
                16,
                pageModifier(),
                VelaText.of(todayLabel(), EYEBROW, Modifier.none()),
                VelaText.of("Make today feel lighter.", DISPLAY, Modifier.none()),
                VelaText.of(
                        "A calm plan, a few meaningful wins, and enough room to breathe.",
                        BODY,
                        Modifier.none()),
                focusStatus,
                focusCard(),
                sectionHeader("Today", completedTasks() + " of 3 complete"),
                taskList(),
                VelaGrid.columns(
                        2,
                        12,
                        Modifier.none().fillMaxWidth(),
                        waterCard(),
                        energyCard()),
                quietCard(),
                VelaSpacer.vertical(12));
    }

    private ViewNode focusCard() {
        return VelaCard.of(
                ComponentVariant.PRIMARY,
                Modifier.none()
                        .fillMaxWidth()
                        .background("#17211D")
                        .foreground("#FFFFFF")
                        .padding(20)
                        .cornerRadius(8),
                VelaRow.of(
                        Arrangement.SPACE_BETWEEN,
                        Alignment.CENTER,
                        8,
                        Modifier.none().fillMaxWidth(),
                        VelaChip.of("DEEP WORK", ComponentVariant.WARNING, null, Modifier.none()),
                        VelaBadge.of(focusSessions + " sessions", ComponentVariant.SUCCESS, Modifier.none())),
                VelaText.of(
                        focusActive ? "Stay with one thing." : "Ready for a clear hour?",
                        TextStyle.defaultStyle().fontSize(24).fontWeight(700).color("#FFFFFF"),
                        Modifier.none()),
                VelaText.of(
                        focusActive
                                ? "Notifications are tucked away. You have this."
                                : "Start a focused block and move the important work forward.",
                        TextStyle.defaultStyle().fontSize(14).color("#C8D2CC").lineHeight(1.35),
                        Modifier.none()),
                VelaProgressIndicator.determinate(
                        Math.min(focusSessions / 4.0, 1),
                        VelaProgressIndicator.Style.LINEAR,
                        Modifier.none().fillMaxWidth().foreground("#E9B44C")),
                VelaButton.of(
                        focusActive ? "Finish focus" : "Start focus",
                        focusActive ? ComponentVariant.SECONDARY : ComponentVariant.WARNING,
                        true,
                        focusActive ? this::completeFocus : this::startFocus,
                        Modifier.none().fillMaxWidth()));
    }

    private ViewNode taskList() {
        return VelaCard.of(
                ComponentVariant.OUTLINED,
                Modifier.none().fillMaxWidth().background("#FFFFFF").padding(8).cornerRadius(8),
                VelaList.of(
                        true,
                        Modifier.none().fillMaxWidth(),
                        task("Shape the launch plan", "09:30", planTaskDone, value -> updatePlanTask(value)),
                        task("Review the mobile experience", "13:00", designTaskDone, value -> updateDesignTask(value)),
                        task("Take a screen-free walk", "18:30", walkTaskDone, value -> updateWalkTask(value))));
    }

    private ViewNode task(String label, String time, boolean checked, java.util.function.Consumer<Boolean> onChange) {
        return VelaRow.of(
                Arrangement.SPACE_BETWEEN,
                Alignment.CENTER,
                10,
                Modifier.none().fillMaxWidth().padding(8),
                VelaCheckbox.of(label, checked, true, onChange, Modifier.none().weight(1)),
                VelaBadge.of(time, ComponentVariant.GHOST, Modifier.none()));
    }

    private ViewNode waterCard() {
        return VelaCard.of(
                ComponentVariant.OUTLINED,
                Modifier.none().background("#EAF2F8").padding(14).cornerRadius(8),
                VelaText.of("Hydration", CARD_TITLE, Modifier.none()),
                VelaText.of(waterGlasses + " / 8 glasses", BODY, Modifier.none()),
                VelaProgressIndicator.determinate(
                        waterGlasses / 8.0,
                        VelaProgressIndicator.Style.LINEAR,
                        Modifier.none().fillMaxWidth().foreground("#3D6FB6")),
                VelaButton.of(
                        "+ Glass",
                        ComponentVariant.GHOST,
                        waterGlasses < 8,
                        this::addWater,
                        Modifier.none().fillMaxWidth()));
    }

    private ViewNode energyCard() {
        return VelaCard.of(
                ComponentVariant.OUTLINED,
                Modifier.none().background("#FFF4E5").padding(14).cornerRadius(8),
                VelaText.of("Energy", CARD_TITLE, Modifier.none()),
                VelaText.of(Math.round(energy) + "% steady", BODY, Modifier.none()),
                VelaSlider.of("Energy", energy, 0, 100, 1, this::updateEnergy, Modifier.none().fillMaxWidth()),
                VelaChip.of(energyLabel(), ComponentVariant.WARNING, null, Modifier.none()));
    }

    private ViewNode quietCard() {
        return VelaCard.of(
                ComponentVariant.OUTLINED,
                Modifier.none().fillMaxWidth().background("#FFFFFF").padding(16).cornerRadius(8),
                VelaRow.of(
                        Arrangement.SPACE_BETWEEN,
                        Alignment.CENTER,
                        12,
                        Modifier.none().fillMaxWidth(),
                        VelaColumn.of(
                                Modifier.none().weight(1),
                                VelaText.of("Quiet notifications", CARD_TITLE, Modifier.none()),
                                VelaText.of("Keep non-essential alerts out of your day.", BODY, Modifier.none())),
                        VelaSwitch.of(
                                "Quiet notifications",
                                quietNotifications,
                                true,
                                this::updateQuietNotifications,
                                Modifier.none())));
    }

    private ViewNode insightsPage() {
        return VelaColumn.of(
                Arrangement.START,
                Alignment.STRETCH,
                16,
                pageModifier(),
                VelaText.of("YOUR RHYTHM", EYEBROW, Modifier.none()),
                VelaText.of("Progress without pressure.", DISPLAY, Modifier.none()),
                VelaText.of("A useful view of your week, without turning life into a spreadsheet.", BODY, Modifier.none()),
                VelaCard.of(
                        ComponentVariant.SUCCESS,
                        Modifier.none().fillMaxWidth().background("#DDEFE5").padding(20).cornerRadius(8),
                        VelaBadge.of("7 DAY STREAK", ComponentVariant.SUCCESS, Modifier.none()),
                        VelaText.of("82%", TextStyle.defaultStyle().fontSize(42).fontWeight(700).color("#175A3E"), Modifier.none()),
                        VelaText.of("of your gentle weekly target", BODY, Modifier.none()),
                        VelaProgressIndicator.determinate(
                                0.82, VelaProgressIndicator.Style.LINEAR, Modifier.none().fillMaxWidth())),
                sectionHeader("This week", "Small wins add up"),
                VelaTable.of(
                        List.of("Day", "Focus", "Energy"),
                        List.of(
                                List.of("Mon", "2 blocks", "74%"),
                                List.of("Tue", "3 blocks", "81%"),
                                List.of("Wed", "2 blocks", "72%"),
                                List.of("Thu", "4 blocks", "86%"),
                                List.of("Fri", focusSessions + " blocks", Math.round(energy) + "%")),
                        Modifier.none().fillMaxWidth().background("#FFFFFF").padding(12).cornerRadius(8)),
                VelaCard.of(
                        ComponentVariant.OUTLINED,
                        Modifier.none().fillMaxWidth().background("#FFFFFF").padding(16).cornerRadius(8),
                        VelaText.of("Weekly pace", CARD_TITLE, Modifier.none()),
                        VelaText.of("Choose the rhythm you want Daymark to encourage.", BODY, Modifier.none()),
                        VelaSegmentedControl.of(
                                List.of("Gentle", "Balanced", "Ambitious"),
                                weekMode,
                                this::updateWeekMode,
                                Modifier.none().fillMaxWidth())),
                VelaSpacer.vertical(12));
    }

    private ViewNode profilePage() {
        return VelaColumn.of(
                Arrangement.START,
                Alignment.STRETCH,
                16,
                pageModifier(),
                VelaText.of("YOUR SPACE", EYEBROW, Modifier.none()),
                VelaText.of("Built around your pace.", DISPLAY, Modifier.none()),
                VelaCard.of(
                        ComponentVariant.OUTLINED,
                        Modifier.none().fillMaxWidth().background("#FFFFFF").padding(20).cornerRadius(8),
                        VelaRow.of(
                                Arrangement.START,
                                Alignment.CENTER,
                                14,
                                Modifier.none().fillMaxWidth(),
                                VelaAvatar.initials("AR", 58, Modifier.none()),
                                VelaColumn.of(
                                        VelaText.of("Aritra", SECTION, Modifier.none()),
                                        VelaText.of("Thoughtful builder · Week 14", BODY, Modifier.none()))),
                        VelaDivider.horizontal(),
                        VelaKeyValue.of(
                                Map.of(
                                        "Current pace", weekMode,
                                        "Focus sessions", Integer.toString(focusSessions),
                                        "Tasks completed", Integer.toString(completedTasks()),
                                        "Hydration", waterGlasses + " of 8"),
                                Modifier.none().fillMaxWidth())),
                VelaCard.of(
                        ComponentVariant.OUTLINED,
                        Modifier.none().fillMaxWidth().background("#FFFFFF").padding(8).cornerRadius(8),
                        VelaListItem.of(
                                "Quiet notifications",
                                quietNotifications ? "Enabled" : "Disabled",
                                null,
                                VelaSwitch.of(
                                        "Quiet notifications",
                                        quietNotifications,
                                        true,
                                        this::updateQuietNotifications,
                                        Modifier.none()),
                                null,
                                Modifier.none()),
                        VelaDivider.horizontal(),
                        VelaListItem.of(
                                "Daily reminder",
                                "Every morning at 8:30",
                                null,
                                VelaBadge.of("ON", ComponentVariant.SUCCESS, Modifier.none()),
                                null,
                                Modifier.none())),
                VelaAlert.of(
                        "Everything stays on this device",
                        "This showcase does not request network access or collect personal data.",
                        ComponentVariant.INFO,
                        null,
                        Modifier.none().fillMaxWidth()),
                VelaSpacer.vertical(12));
    }

    private ViewNode sectionHeader(String title, String detail) {
        return VelaRow.of(
                Arrangement.SPACE_BETWEEN,
                Alignment.CENTER,
                8,
                Modifier.none().fillMaxWidth(),
                VelaText.of(title, SECTION, Modifier.none()),
                VelaText.of(detail, EYEBROW, Modifier.none()));
    }

    private Modifier pageModifier() {
        return Modifier.none().fillMaxWidth().padding(20).background("#F4F6F2");
    }

    private String todayLabel() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.ENGLISH)).toUpperCase(Locale.ENGLISH);
    }

    private int completedTasks() {
        return (planTaskDone ? 1 : 0) + (designTaskDone ? 1 : 0) + (walkTaskDone ? 1 : 0);
    }

    private String energyLabel() {
        if (energy >= 80) {
            return "HIGH";
        }
        if (energy >= 50) {
            return "STEADY";
        }
        return "REST";
    }

    private void selectPage(int page) {
        if (selectedPage != page) {
            selectedPage = page;
            markDirty();
        }
    }

    private void startFocus() {
        focusActive = true;
        markDirty();
    }

    private void stopFocus() {
        focusActive = false;
        markDirty();
    }

    private void completeFocus() {
        focusActive = false;
        focusSessions++;
        markDirty();
    }

    private void addWater() {
        if (waterGlasses < 8) {
            waterGlasses++;
            markDirty();
        }
    }

    private void updateEnergy(double value) {
        energy = value;
        markDirty();
    }

    private void updateQuietNotifications(boolean enabled) {
        quietNotifications = enabled;
        markDirty();
    }

    private void updatePlanTask(boolean completed) {
        planTaskDone = completed;
        markDirty();
    }

    private void updateDesignTask(boolean completed) {
        designTaskDone = completed;
        markDirty();
    }

    private void updateWalkTask(boolean completed) {
        walkTaskDone = completed;
        markDirty();
    }

    private void updateWeekMode(String mode) {
        weekMode = mode;
        markDirty();
    }
}
