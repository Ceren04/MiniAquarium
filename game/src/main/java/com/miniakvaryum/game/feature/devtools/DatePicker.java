package com.miniakvaryum.game.feature.devtools;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.core.account.AccountService;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Developer tools: a small panel above the dev bar to pretend another date, for example the
 * child's birthday. Day, month and year each have a "+" and a "-" button; "Birthday" jumps to
 * the child's birthday in the shown year, "Real date" goes back to today. Every change is used
 * at once ({@link AccountService#setTodayForDevelopment}); screens show it the next time they
 * open.
 *
 * <p>Like the bar, the buttons are not touchable themselves; the feature forwards world taps to
 * {@link #handleTap}.
 */
final class DatePicker extends Group {

    private static final float WIDTH = 540f;
    private static final float HEIGHT = 350f;
    private static final float BOTTOM = 96f;
    private static final float SMALL_BUTTON = 64f;
    private static final float BUTTON_HEIGHT = 56f;
    private static final float WIDE_BUTTON = 160f;
    private static final float COLUMN_GAP = 170f;
    private static final float SMALL_TEXT = 1.2f;
    private static final Color PANEL_COLOR = new Color(0.30f, 0.06f, 0.06f, 0.92f);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    private record Button(Rectangle area, Runnable action) {
    }

    private final Skin skin;
    private final AccountService accounts;
    private final Supplier<LocalDate> birthday;
    private final List<Button> buttons = new ArrayList<>();
    private final Label day;
    private final Label month;
    private final Label year;
    private final Rectangle area;
    private Runnable onChange = () -> { };
    private Runnable onOpen = () -> { };

    /**
     * @param worldWidth width of the world, to center the panel
     * @param birthday   the signed-in child's birthday
     */
    DatePicker(Skin skin, float worldWidth, AccountService accounts, Supplier<LocalDate> birthday) {
        this.skin = skin;
        this.accounts = accounts;
        this.birthday = birthday;
        float left = (worldWidth - WIDTH) / 2f;
        area = new Rectangle(left, BOTTOM, WIDTH, HEIGHT);
        setTouchable(Touchable.disabled);

        Image background = new Image(skin.newDrawable(UiSkin.WHITE, PANEL_COLOR));
        background.setBounds(left, BOTTOM, WIDTH, HEIGHT);
        addActor(background);

        Label title = new Label(DevToolsStrings.DATE_TITLE, skin);
        title.setAlignment(Align.center);
        title.setBounds(left, BOTTOM + HEIGHT - 50f, WIDTH, 40f);
        addActor(title);

        float center = left + WIDTH / 2f;
        day = column(center - COLUMN_GAP, DevToolsStrings.DATE_DAY, d -> d.plusDays(1), d -> d.minusDays(1));
        month = column(center, DevToolsStrings.DATE_MONTH, d -> d.plusMonths(1), d -> d.minusMonths(1));
        year = column(center + COLUMN_GAP, DevToolsStrings.DATE_YEAR, d -> d.plusYears(1), d -> d.minusYears(1));

        float rowY = BOTTOM + 16f;
        float gap = (WIDTH - 3 * WIDE_BUTTON) / 4f;
        button(DevToolsStrings.DATE_BIRTHDAY, left + gap, rowY, WIDE_BUTTON, this::goToBirthday);
        button(DevToolsStrings.DATE_REAL, left + 2 * gap + WIDE_BUTTON, rowY, WIDE_BUTTON,
                () -> accounts.setTodayForDevelopment(null));
        button(DevToolsStrings.DATE_CLOSE, left + 3 * gap + 2 * WIDE_BUTTON, rowY, WIDE_BUTTON,
                () -> setVisible(false));

        setVisible(false);
        refresh();
    }

    /** One column: name, "+", the value, "-". Returns the value label. */
    private Label column(float centerX, String name, UnaryOperator<LocalDate> up, UnaryOperator<LocalDate> down) {
        Label caption = new Label(name, skin);
        caption.setFontScale(SMALL_TEXT);
        caption.setAlignment(Align.center);
        caption.setBounds(centerX - 60f, BOTTOM + 255f, 120f, 30f);
        addActor(caption);
        button(DevToolsStrings.PLUS, centerX - SMALL_BUTTON / 2f, BOTTOM + 186f, SMALL_BUTTON,
                () -> accounts.setTodayForDevelopment(up.apply(accounts.today())));
        Label value = new Label("", skin);
        value.setAlignment(Align.center);
        value.setBounds(centerX - 70f, BOTTOM + 142f, 140f, 40f);
        addActor(value);
        button(DevToolsStrings.MINUS, centerX - SMALL_BUTTON / 2f, BOTTOM + 84f, SMALL_BUTTON,
                () -> accounts.setTodayForDevelopment(down.apply(accounts.today())));
        return value;
    }

    private void button(String text, float x, float y, float width, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.setBounds(x, y, width, BUTTON_HEIGHT);
        button.setTouchable(Touchable.disabled);
        addActor(button);
        buttons.add(new Button(new Rectangle(x, y, width, BUTTON_HEIGHT), () -> {
            action.run();
            refresh();
        }));
    }

    /** The child's birthday in the year that is shown now (28 February when 29 February is missing). */
    private void goToBirthday() {
        LocalDate born = birthday.get();
        int shownYear = accounts.today().getYear();
        accounts.setTodayForDevelopment(MonthDay.from(born).atYear(shownYear));
    }

    private void refresh() {
        LocalDate today = accounts.today();
        day.setText(String.valueOf(today.getDayOfMonth()));
        month.setText(MONTH.format(today));
        year.setText(String.valueOf(today.getYear()));
        onChange.run();
    }

    /** Called after every change, for example to update the "Date" button. */
    void setOnChange(Runnable onChange) {
        this.onChange = onChange;
        onChange.run();
    }

    /** Text for the "Date" button: the pretend date, or "Date" for the real one. */
    String buttonText() {
        return accounts.isTodayPretended() ? SHORT.format(accounts.today()) : DevToolsStrings.DATE;
    }

    /** Called when the panel opens, for example to close the other panel. */
    void setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen;
    }

    /** Opens or closes the panel. */
    void toggle() {
        refresh();
        if (!isVisible()) {
            onOpen.run();
        }
        setVisible(!isVisible());
    }

    /**
     * Handles a tap in world coordinates while the dev bar is shown.
     *
     * @return true if the tap was inside the open panel; the tap is then used up
     */
    boolean handleTap(float x, float y) {
        if (!isVisible() || !area.contains(x, y)) {
            return false;
        }
        for (Button button : buttons) {
            if (button.area().contains(x, y)) {
                button.action().run();
                break;
            }
        }
        return true;
    }
}
