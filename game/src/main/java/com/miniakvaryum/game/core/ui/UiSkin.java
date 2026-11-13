package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 * Builds the look of all widgets in code, so the game needs no skin files.
 *
 * <p>Style names: {@code "default"} for labels, buttons and text fields; {@code "title"} for big
 * labels; {@code "message"} for the message box; {@code "toggle"} for buttons that stay highlighted while switched on. The 1 x 1 white
 * image {@code "white"} can be tinted into any plain rectangle with
 * {@code skin.newDrawable("white", color)}.
 */
public final class UiSkin {

    /** Name of the 1 x 1 white image in the skin. */
    public static final String WHITE = "white";

    /** Style name for big headline labels. */
    public static final String TITLE = "title";

    /** Style name for the short message label: white text on a dark box. */
    public static final String MESSAGE = "message";

    /** Style name for buttons that show an "on" state. */
    public static final String TOGGLE = "toggle";

    private static final Color BUTTON = new Color(0.10f, 0.32f, 0.52f, 1f);
    private static final Color BUTTON_OVER = new Color(0.14f, 0.40f, 0.63f, 1f);
    private static final Color BUTTON_DOWN = new Color(0.06f, 0.22f, 0.38f, 1f);
    private static final Color BUTTON_ON = new Color(0.90f, 0.50f, 0.10f, 1f);

    private UiSkin() {
    }

    /** Creates the skin. The caller owns it and must dispose it when the game ends. */
    public static Skin create() {
        Skin skin = new Skin();

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        skin.add(WHITE, new Texture(pixel));
        pixel.dispose();

        // The font that ships inside libGDX; scaled up because it is small.
        BitmapFont font = scaledFont(1.6f);
        BitmapFont titleFont = scaledFont(2.6f);
        skin.add("default", font);
        skin.add(TITLE, titleFont);

        skin.add("default", new Label.LabelStyle(font, Color.WHITE));
        skin.add(TITLE, new Label.LabelStyle(titleFont, Color.WHITE));
        Label.LabelStyle message = new Label.LabelStyle(font, Color.WHITE);
        message.background = skin.newDrawable(WHITE, new Color(0f, 0f, 0f, 0.75f));
        skin.add(MESSAGE, message);

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = font;
        button.fontColor = Color.WHITE;
        button.up = skin.newDrawable(WHITE, BUTTON);
        button.over = skin.newDrawable(WHITE, BUTTON_OVER);
        button.down = skin.newDrawable(WHITE, BUTTON_DOWN);
        skin.add("default", button);

        TextButton.TextButtonStyle toggle = new TextButton.TextButtonStyle(button);
        toggle.checked = skin.newDrawable(WHITE, BUTTON_ON);
        skin.add(TOGGLE, toggle);

        TextField.TextFieldStyle field = new TextField.TextFieldStyle();
        field.font = font;
        field.fontColor = Color.BLACK;
        field.background = skin.newDrawable(WHITE, Color.WHITE);
        field.cursor = skin.newDrawable(WHITE, Color.BLACK);
        field.selection = skin.newDrawable(WHITE, new Color(0.6f, 0.8f, 1f, 1f));
        skin.add("default", field);

        return skin;
    }

    private static BitmapFont scaledFont(float scale) {
        BitmapFont font = new BitmapFont();
        font.getData().setScale(scale);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return font;
    }
}
