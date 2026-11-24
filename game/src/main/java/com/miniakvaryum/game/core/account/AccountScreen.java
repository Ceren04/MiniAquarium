package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.miniakvaryum.domain.core.account.Picture;
import com.miniakvaryum.domain.core.account.SafetyColor;
import com.miniakvaryum.game.core.ui.BaseScreen;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.function.Consumer;

/** Base of the account screens: shared widgets (fields, pictures, color circles). */
abstract class AccountScreen extends BaseScreen {

    static final Color ERROR = new Color(1f, 0.72f, 0.45f, 1f);
    static final Color HINT = new Color(1f, 1f, 1f, 0.7f);
    static final float FIELD_WIDTH = 380f;
    static final float FIELD_HEIGHT = 52f;
    static final float WIDE_BUTTON = 200f;
    private static final float DISABLED_ALPHA = 0.4f;

    /** Exact colors of the safety colors, in {@link SafetyColor} order. */
    private static final Color[] SAFETY = {
        Color.valueOf("E53935"), Color.valueOf("FF9F2E"), Color.valueOf("FDD835"), Color.valueOf("43A047"),
        Color.valueOf("1DB9B0"), Color.valueOf("1E88E5"), Color.valueOf("8E24AA"), Color.valueOf("F06292"),
        Color.valueOf("8D6E63"), Color.valueOf("9E9E9E"),
    };

    /** Circle colors for the first letter when no picture was chosen. */
    private static final Color[] INITIAL_COLORS = {
        Color.valueOf("FF9F2E"), Color.valueOf("8ACBEE"), Color.valueOf("F2B84B"), Color.valueOf("43A047"),
        Color.valueOf("8E24AA"), Color.valueOf("F06292"),
    };

    protected final AccountFlow flow;

    AccountScreen(AccountFlow flow) {
        super(flow.skin());
        this.flow = flow;
    }

    @Override
    protected Color background() {
        return flow.background();
    }

    // ---------------------------------------------------------------- texts

    Label title(String text) {
        return new Label(text, skin, UiSkin.TITLE);
    }

    Label text(String text) {
        Label label = new Label(text, skin);
        label.setAlignment(Align.center);
        return label;
    }

    Label small(String text, Color color) {
        Label label = new Label(text, skin);
        label.setFontScale(0.9f);
        label.setColor(color);
        return label;
    }

    // ---------------------------------------------------------------- inputs

    TextField field(int maxLength, boolean password, Runnable onChange) {
        TextField field = new TextField("", flow.fieldStyle());
        field.setMaxLength(maxLength);
        if (password) {
            field.setPasswordCharacter('*');
            field.setPasswordMode(true);
        }
        field.setTextFieldListener((f, c) -> {
            // Enter and Tab are handled by the screens; they do not change the text.
            if (c != '\r' && c != '\n' && c != '\t') {
                onChange.run();
            }
        });
        return field;
    }

    /** A small "Show" / "Hide" button that toggles the password mode of the given fields. */
    TextButton showHide(TextField... fields) {
        TextButton toggle = new TextButton(AccountStrings.SHOW, skin);
        toggle.pad(0, UiConfig.GAP, 0, UiConfig.GAP);
        toggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean hidden = fields[0].isPasswordMode();
                for (TextField field : fields) {
                    field.setPasswordMode(!hidden);
                }
                toggle.setText(hidden ? AccountStrings.HIDE : AccountStrings.SHOW);
            }
        });
        return toggle;
    }

    /** Resets a "Show" / "Hide" button and its fields to hidden. */
    static void hide(TextButton toggle, TextField... fields) {
        for (TextField field : fields) {
            field.setPasswordMode(true);
        }
        toggle.setText(AccountStrings.SHOW);
    }

    static void setEnabled(TextButton button, boolean enabled) {
        button.setDisabled(!enabled);
        button.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
        button.getColor().a = enabled ? 1f : DISABLED_ALPHA;
    }

    /** Toggle button for "Remember me"; the state is the checked flag of the button. */
    TextButton rememberToggle() {
        TextButton toggle = new TextButton(AccountStrings.REMEMBER_ME_OFF, skin, UiSkin.TOGGLE);
        toggle.pad(0, UiConfig.GAP * 2, 0, UiConfig.GAP * 2);
        toggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggle.setText(toggle.isChecked() ? AccountStrings.REMEMBER_ME_ON : AccountStrings.REMEMBER_ME_OFF);
            }
        });
        return toggle;
    }

    static void setRemember(TextButton toggle, boolean on) {
        toggle.setChecked(on);
        toggle.setText(on ? AccountStrings.REMEMBER_ME_ON : AccountStrings.REMEMBER_ME_OFF);
    }

    // ---------------------------------------------------------------- pictures

    /** The picture of an account: an avatar image, or the first letter in a colored circle. */
    Actor picture(Picture picture, String firstName, float size) {
        if (picture.kind() == Picture.Kind.AVATAR) {
            Image image = new Image(flow.avatar(picture.avatarId()));
            image.setScaling(Scaling.fit);
            return image;
        }
        String name = firstName == null ? "" : firstName.trim();
        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        Color color = INITIAL_COLORS[Math.floorMod(name.hashCode(), INITIAL_COLORS.length)];
        Image circle = new Image(flow.circle((int) size, color));
        circle.setScaling(Scaling.fit);
        Label label = new Label(letter, skin, UiSkin.TITLE);
        label.setAlignment(Align.center);
        label.setFontScale(size / 100f);
        Stack stack = new Stack();
        stack.add(circle);
        stack.add(label);
        return stack;
    }

    /** A clickable avatar or color circle with a selection ring. */
    static final class Choice extends Stack {
        private final Image ring;

        Choice(Image ring, Actor content, Runnable onClick) {
            this.ring = ring;
            ring.setVisible(false);
            add(ring);
            Table inner = new Table();
            inner.add(content).grow().pad(6);
            add(inner);
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onClick.run();
                }
            });
        }

        void setSelected(boolean selected) {
            ring.setVisible(selected);
        }
    }

    Image ring(int size) {
        Image ring = new Image(flow.circle(size, Color.WHITE));
        ring.setScaling(Scaling.fit);
        return ring;
    }

    /** Ten color circles in two rows of five, with their names. Calls back with the picked color. */
    Table colorGrid(Choice[] choices, Consumer<SafetyColor> onPick) {
        Table grid = new Table();
        SafetyColor[] colors = SafetyColor.values();
        for (int i = 0; i < colors.length; i++) {
            SafetyColor color = colors[i];
            Image circle = new Image(flow.circle(96, SAFETY[i]));
            circle.setScaling(Scaling.fit);
            choices[i] = new Choice(ring(108), circle, () -> onPick.accept(color));
            Table cell = new Table();
            cell.add(choices[i]).size(108).row();
            cell.add(small(AccountStrings.COLOR_NAMES[i], Color.WHITE)).padTop(4);
            grid.add(cell).pad(UiConfig.GAP);
            if (i == colors.length / 2 - 1) {
                grid.row();
            }
        }
        return grid;
    }

    static void select(Choice[] choices, int index) {
        for (int i = 0; i < choices.length; i++) {
            choices[i].setSelected(i == index);
        }
    }

    /** Wait text for a lock: seconds below two minutes, otherwise minutes. */
    static String lockText(long remainingMillis) {
        long seconds = (remainingMillis + 999) / 1_000;
        if (seconds < 120) {
            return String.format(AccountStrings.LOCKED_SECONDS, seconds);
        }
        return String.format(AccountStrings.LOCKED_MINUTES, (seconds + 59) / 60);
    }
}
