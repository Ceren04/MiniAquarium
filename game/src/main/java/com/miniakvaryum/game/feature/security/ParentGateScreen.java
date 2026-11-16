package com.miniakvaryum.game.feature.security;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.security.api.PinState;
import com.miniakvaryum.domain.feature.security.api.VerifyResult;
import com.miniakvaryum.game.core.CoreStrings;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.BaseScreen;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;

/**
 * The PIN gate in front of the parent area (specification section 10.5).
 *
 * <ul>
 *   <li>No PIN set yet: the parent types a new PIN twice ({@link SecurityStrings#SET_PIN},
 *       {@link SecurityStrings#REPEAT_PIN}). When both match, the PIN is set and the gate
 *       continues as if it had just been entered.</li>
 *   <li>Otherwise: {@link SecurityStrings#ENTER_PIN}; OK calls {@code ParentSession.unlock}.</li>
 *   <li>Input: an on-screen number pad (0 to 9, clear, OK) and the digit keys of the keyboard;
 *       Backspace deletes, Enter confirms, Escape goes back. Typed digits are shown as dots.</li>
 *   <li>A wrong PIN shows the attempts left; a lockout shows the remaining seconds and counts
 *       down with game time.</li>
 *   <li>A correct PIN opens the screen whose id was passed in.</li>
 * </ul>
 */
final class ParentGateScreen extends BaseScreen {

    /** Steps of the gate. */
    private enum Mode { SET_FIRST, SET_CONFIRM, ENTER }

    private static final int DOT_SIZE = 28;
    private static final float KEY_WIDTH = 120f;
    private static final float KEY_HEIGHT = 80f;
    private static final float MESSAGE_SECONDS = 2.5f;
    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final Color DOT_TYPED = Color.WHITE;
    private static final Color DOT_EMPTY = new Color(1f, 1f, 1f, 0.25f);

    private final UiContext ui;
    private final String afterUnlockScreenId;
    private final PinService pins;
    private final ParentSession parent;
    /** The game uses the default security config (see DomainFeatures); the domain checks again. */
    private final int pinLength = SecurityConfig.defaults().pinLength();

    private final Label prompt;
    private final Label status;
    private final Image[] dots;

    private final StringBuilder typed = new StringBuilder();
    private Mode mode = Mode.ENTER;
    /** The first entry while a new PIN is being set; cleared as soon as it is no longer needed. */
    private String firstEntry;
    /** Game time at which the current lockout ends; 0 while not locked. */
    private long lockedUntilGameMillis;

    /**
     * @param afterUnlockScreenId id of the screen to open after a correct PIN
     */
    ParentGateScreen(UiContext ui, String afterUnlockScreenId) {
        super(ui.skin());
        this.ui = ui;
        this.afterUnlockScreenId = afterUnlockScreenId;
        this.pins = ui.session().get(PinService.class);
        this.parent = ui.session().get(ParentSession.class);

        Table root = newRootTable();
        root.add(new Label(SecurityStrings.GATE_TITLE, skin, UiSkin.TITLE)).padBottom(UiConfig.GAP * 2).row();

        prompt = new Label("", skin);
        root.add(prompt).padBottom(UiConfig.GAP * 2).row();

        Texture dotTexture = ui.shapes().circle(DOT_SIZE, Color.WHITE);
        Table dotRow = new Table();
        dots = new Image[pinLength];
        for (int i = 0; i < pinLength; i++) {
            dots[i] = new Image(dotTexture);
            dotRow.add(dots[i]).size(DOT_SIZE).pad(0, UiConfig.GAP, 0, UiConfig.GAP);
        }
        root.add(dotRow).padBottom(UiConfig.GAP * 2).row();

        status = new Label("", skin);
        root.add(status).height(UiConfig.MIN_TOUCH_SIZE / 2).padBottom(UiConfig.GAP).row();

        root.add(buildPad()).padBottom(UiConfig.GAP * 2).row();
        root.add(button(CoreStrings.BACK, ui.nav()::showMain)).minSize(UiConfig.MIN_TOUCH_SIZE * 3, UiConfig.MIN_TOUCH_SIZE);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyTyped(InputEvent event, char character) {
                if (character >= '0' && character <= '9') {
                    guarded(() -> typeDigit(character));
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                switch (keycode) {
                    case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> guarded(ParentGateScreen.this::submit);
                    case Input.Keys.BACKSPACE -> deleteLast();
                    case Input.Keys.ESCAPE -> ui.nav().showMain();
                    default -> {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    /** 1 2 3 / 4 5 6 / 7 8 9 / Clear 0 OK; every key is larger than the minimum touch size. */
    private Table buildPad() {
        Table pad = new Table();
        String[] rows = {"123", "456", "789"};
        for (String row : rows) {
            for (char digit : row.toCharArray()) {
                addKey(pad, String.valueOf(digit), () -> typeDigit(digit));
            }
            pad.row();
        }
        addKey(pad, SecurityStrings.CLEAR, this::clearTyped);
        addKey(pad, "0", () -> typeDigit('0'));
        addKey(pad, SecurityStrings.OK, this::submit);
        return pad;
    }

    private void addKey(Table pad, String label, Runnable action) {
        pad.add(button(label, action)).size(KEY_WIDTH, KEY_HEIGHT).pad(UiConfig.GAP / 2);
    }

    @Override
    protected void onShow() {
        clearTyped();
        firstEntry = null;
        status.setText("");
        mode = pins.isPinSet() ? Mode.ENTER : Mode.SET_FIRST;
        // A lockout that is still running (also after a restart) is shown right away.
        long now = ui.session().time().nowMillis();
        lockedUntilGameMillis = pins.state().map(PinState::lockedUntilGameMillis).filter(until -> until > now).orElse(0L);
        refreshPrompt();
    }

    @Override
    protected void update(float delta) {
        if (lockedUntilGameMillis <= 0) {
            return;
        }
        long remaining = lockedUntilGameMillis - ui.session().time().nowMillis();
        if (remaining > 0) {
            long seconds = (remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND;
            status.setText(String.format(SecurityStrings.LOCKED, seconds));
        } else {
            lockedUntilGameMillis = 0;
            status.setText("");
        }
    }

    // ------------------------------------------------------------ input

    private void typeDigit(char digit) {
        if (typed.length() < pinLength) {
            typed.append(digit);
            refreshDots();
        }
    }

    private void deleteLast() {
        if (typed.length() > 0) {
            typed.setLength(typed.length() - 1);
            refreshDots();
        }
    }

    private void clearTyped() {
        typed.setLength(0);
        refreshDots();
    }

    /** OK button or Enter key. */
    private void submit() {
        if (typed.length() != pinLength) {
            showMessage(String.format(SecurityStrings.WRONG_LENGTH, pinLength), MESSAGE_SECONDS);
            return;
        }
        String pin = typed.toString();
        clearTyped();
        switch (mode) {
            case SET_FIRST -> {
                firstEntry = pin;
                mode = Mode.SET_CONFIRM;
                refreshPrompt();
            }
            case SET_CONFIRM -> {
                String first = firstEntry;
                firstEntry = null;
                if (first.equals(pin)) {
                    pins.setPin(pin);
                    mode = Mode.ENTER;
                    handle(parent.unlock(pin));
                } else {
                    mode = Mode.SET_FIRST;
                    showMessage(SecurityStrings.PINS_DIFFERENT, MESSAGE_SECONDS);
                }
                refreshPrompt();
            }
            case ENTER -> handle(parent.unlock(pin));
        }
    }

    /** Shows the outcome of a PIN check, or opens the protected screen. */
    private void handle(VerifyResult result) {
        switch (result.status()) {
            case OK -> {
                status.setText("");
                ui.nav().show(afterUnlockScreenId);
            }
            case WRONG -> status.setText(String.format(SecurityStrings.WRONG_PIN, result.attemptsLeft()));
            case LOCKED -> {
                lockedUntilGameMillis = ui.session().time().nowMillis() + result.lockedRemainingMillis();
                update(0f);
            }
            case NOT_SET -> {
                mode = Mode.SET_FIRST;
                refreshPrompt();
            }
        }
    }

    // ------------------------------------------------------------ display

    private void refreshPrompt() {
        prompt.setText(switch (mode) {
            case SET_FIRST -> SecurityStrings.SET_PIN;
            case SET_CONFIRM -> SecurityStrings.REPEAT_PIN;
            case ENTER -> SecurityStrings.ENTER_PIN;
        });
    }

    private void refreshDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setColor(i < typed.length() ? DOT_TYPED : DOT_EMPTY);
        }
    }
}
