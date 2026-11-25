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
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.security.api.VerifyResult;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.ArrayList;
import java.util.List;

/**
 * Developer tools: "+5 tasks" for testing the trophy road. After the real parent PIN (typed on
 * this small keypad and checked by {@link ParentSession#unlock}, with the normal wrong-PIN
 * lock), it creates five tasks, reports them done and approves them through {@link TaskService},
 * exactly like a parent would, then locks the parent area again. Nothing skips the PIN.
 *
 * <p>Like the bar, the buttons are not touchable themselves; the feature forwards world taps to
 * {@link #handleTap}.
 */
final class ApproveTasksPanel extends Group {

    static final int TASKS_PER_PRESS = 5;

    private static final float WIDTH = 440f;
    private static final float HEIGHT = 470f;
    private static final float BOTTOM = 96f;
    private static final float KEY_WIDTH = 100f;
    private static final float KEY_HEIGHT = 56f;
    private static final float KEY_GAP = 8f;
    private static final float SMALL_TEXT = 1.15f;
    private static final int MAX_DIGITS = 8;
    private static final long MINUTE = 60_000L;
    private static final Color PANEL_COLOR = new Color(0.30f, 0.06f, 0.06f, 0.92f);
    private static final String[] KEYS = {"1", "2", "3", "4", "5", "6", "7", "8", "9",
        DevToolsStrings.PIN_DELETE, "0", DevToolsStrings.PIN_OK};

    private record Button(Rectangle area, Runnable action) {
    }

    private final TaskService tasks;
    private final ParentSession parent;
    private final PinService pins;
    private final List<Button> buttons = new ArrayList<>();
    private final Rectangle area;
    private final Label pinText;
    private final Label status;
    private final StringBuilder pin = new StringBuilder();
    private Runnable onOpen = () -> { };

    ApproveTasksPanel(Skin skin, float worldWidth, TaskService tasks, ParentSession parent, PinService pins) {
        this.tasks = tasks;
        this.parent = parent;
        this.pins = pins;
        float left = (worldWidth - WIDTH) / 2f;
        float top = BOTTOM + HEIGHT;
        area = new Rectangle(left, BOTTOM, WIDTH, HEIGHT);
        setTouchable(Touchable.disabled);

        Image background = new Image(skin.newDrawable(UiSkin.WHITE, PANEL_COLOR));
        background.setBounds(left, BOTTOM, WIDTH, HEIGHT);
        addActor(background);
        Label title = new Label(DevToolsStrings.PIN_TITLE, skin);
        title.setAlignment(Align.center);
        title.setBounds(left, top - 45f, WIDTH, 36f);
        addActor(title);
        pinText = new Label("", skin);
        pinText.setAlignment(Align.center);
        pinText.setBounds(left, top - 88f, WIDTH, 36f);
        addActor(pinText);

        float keysLeft = left + (WIDTH - (3 * KEY_WIDTH + 2 * KEY_GAP)) / 2f;
        for (int i = 0; i < KEYS.length; i++) {
            String key = KEYS[i];
            float x = keysLeft + (i % 3) * (KEY_WIDTH + KEY_GAP);
            float y = top - 160f - (i / 3) * (KEY_HEIGHT + KEY_GAP);
            button(skin, key, x, y, KEY_WIDTH, () -> press(key));
        }

        status = new Label("", skin);
        status.setFontScale(SMALL_TEXT);
        status.setAlignment(Align.center);
        status.setBounds(left, BOTTOM + 74f, WIDTH, 30f);
        addActor(status);
        button(skin, DevToolsStrings.DATE_CLOSE, left + (WIDTH - 160f) / 2f, BOTTOM + 10f, 160f, () -> setVisible(false));

        setVisible(false);
        refresh();
    }

    private void button(Skin skin, String text, float x, float y, float width, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.setBounds(x, y, width, KEY_HEIGHT);
        button.setTouchable(Touchable.disabled);
        addActor(button);
        buttons.add(new Button(new Rectangle(x, y, width, KEY_HEIGHT), action));
    }

    private void press(String key) {
        if (key.equals(DevToolsStrings.PIN_DELETE)) {
            if (pin.length() > 0) {
                pin.setLength(pin.length() - 1);
            }
        } else if (key.equals(DevToolsStrings.PIN_OK)) {
            submit();
        } else if (pin.length() < MAX_DIGITS) {
            pin.append(key);
        }
        refresh();
    }

    /** Checks the PIN with the real parent gate; on success approves five new tasks. */
    private void submit() {
        String typed = pin.toString();
        pin.setLength(0);
        if (!pins.isPinSet()) {
            status.setText(DevToolsStrings.PIN_NOT_SET);
            return;
        }
        VerifyResult result;
        try {
            result = parent.unlock(typed);
        } catch (IllegalArgumentException e) {
            status.setText(DevToolsStrings.PIN_LENGTH);
            return;
        }
        switch (result.status()) {
            case OK -> {
                try {
                    approveFive();
                } finally {
                    parent.lock();
                }
                status.setText(String.format(DevToolsStrings.TASKS_APPROVED, TASKS_PER_PRESS));
            }
            case WRONG -> status.setText(String.format(DevToolsStrings.PIN_WRONG, result.attemptsLeft()));
            case LOCKED -> status.setText(String.format(DevToolsStrings.PIN_LOCKED,
                    Math.max(1, (result.lockedRemainingMillis() + MINUTE - 1) / MINUTE)));
            case NOT_SET -> status.setText(DevToolsStrings.PIN_NOT_SET);
        }
    }

    private void approveFive() {
        int number = tasks.list().size();
        for (int i = 1; i <= TASKS_PER_PRESS; i++) {
            Task task = tasks.create(String.format(DevToolsStrings.DEV_TASK, number + i), 0, 0);
            tasks.reportDone(task.id());
            tasks.approve(task.id());
        }
    }

    private void refresh() {
        pinText.setText(pin.length() == 0 ? DevToolsStrings.PIN_EMPTY : "*".repeat(pin.length()));
    }

    /** Called when the panel opens, for example to close the date picker. */
    void setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen;
    }

    /** Opens or closes the panel; opening clears the PIN and the last message. */
    void toggle() {
        boolean open = !isVisible();
        if (open) {
            pin.setLength(0);
            status.setText("");
            refresh();
            onOpen.run();
        }
        setVisible(open);
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
