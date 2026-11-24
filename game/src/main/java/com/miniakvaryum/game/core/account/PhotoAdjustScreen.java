package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.miniakvaryum.domain.core.account.Picture;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.UiConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * "Adjust your photo": the photo lies behind a round frame; the child drags it and zooms with
 * "+" / "-", the mouse wheel or two fingers. "Use photo" cuts the circle out (512 x 512,
 * transparent corners) and saves it as {@code photo.png} in the account folder. The original file is only
 * read, never changed, and the photo never leaves the device.
 */
final class PhotoAdjustScreen extends AccountScreen {

    private static final Logger LOG = Logger.getLogger(PhotoAdjustScreen.class.getName());

    static final int OUTPUT_SIZE = 512;
    private static final float FRAME = 380f;
    private static final float FRAME_CENTER_X = UiConfig.VIEW_WIDTH / 2f;
    private static final float FRAME_CENTER_Y = 380f;
    private static final float ZOOM_STEP = 1.15f;
    /** Larger photos are made smaller first; textures cannot be arbitrarily big. */
    private static final int MAX_SOURCE_SIDE = 2048;
    private static final Color SHADE = new Color(0f, 0f, 0f, 0.65f);

    private final UiContext game;
    private final Image photoImage = new Image();
    private Pixmap source;
    private Texture texture;
    private Texture shade;
    private PhotoFrame frame;

    PhotoAdjustScreen(AccountUi accountUi, UiContext game) {
        super(accountUi);
        this.game = game;

        photoImage.setTouchable(Touchable.disabled);
        stage.addActor(photoImage);
        shade = shadeWithHole();
        Image shadeImage = new Image(shade);
        shadeImage.setBounds(0, 0, UiConfig.VIEW_WIDTH, UiConfig.VIEW_HEIGHT);
        shadeImage.setTouchable(Touchable.disabled);
        stage.addActor(shadeImage);

        // Everything over the photo; drag and zoom anywhere outside the buttons.
        Actor touchArea = new Actor();
        touchArea.setBounds(0, 0, UiConfig.VIEW_WIDTH, UiConfig.VIEW_HEIGHT);
        touchArea.addListener(new ActorGestureListener() {
            private float lastDistance;

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                if (frame != null) {
                    frame.move(deltaX, deltaY);
                    layoutPhoto();
                }
            }

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance) {
                if (frame != null && lastDistance > 0) {
                    frame.zoom(distance / lastDistance);
                    layoutPhoto();
                }
                lastDistance = distance;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                lastDistance = 0;
            }
        });
        touchArea.addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                zoom(amountY > 0 ? 1f / ZOOM_STEP : ZOOM_STEP);
                return true;
            }
        });
        stage.addActor(touchArea);

        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.ADJUST_TITLE)).padBottom(UiConfig.GAP / 2f).row();
        root.add(small(AccountStrings.ADJUST_TEXT, Color.WHITE)).row();
        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, this::back)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add().expandX();
        buttons.add(button("-", () -> zoom(1f / ZOOM_STEP))).size(UiConfig.MIN_TOUCH_SIZE * 1.3f, UiConfig.MIN_TOUCH_SIZE);
        buttons.add(button("+", () -> zoom(ZOOM_STEP))).size(UiConfig.MIN_TOUCH_SIZE * 1.3f, UiConfig.MIN_TOUCH_SIZE)
                .padLeft(UiConfig.GAP);
        buttons.add().expandX();
        buttons.add(button(AccountStrings.USE_PHOTO, this::usePhoto)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons).growX().expandY().bottom();
        // The table only lays out; touches go through to the drag area where there is no button.
        root.setTouchable(Touchable.childrenOnly);
        root.toFront();
    }

    /**
     * Loads the picked file. Call right after showing this screen.
     *
     * @param file a png or jpg picked by the child
     */
    void open(Path file) {
        release();
        try {
            Pixmap loaded = new Pixmap(Gdx.files.absolute(file.toAbsolutePath().toString()));
            source = smaller(loaded);
        } catch (GdxRuntimeException e) {
            LOG.log(Level.WARNING, "Could not read the picked picture " + file, e);
            game.nav().back();
            return;
        }
        texture = new Texture(source);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        photoImage.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(texture));
        frame = new PhotoFrame(source.getWidth(), source.getHeight(), FRAME);
        layoutPhoto();
    }

    /** Scales a very large photo down so it fits in a texture; keeps the original if small enough. */
    private static Pixmap smaller(Pixmap loaded) {
        int longest = Math.max(loaded.getWidth(), loaded.getHeight());
        if (longest <= MAX_SOURCE_SIDE) {
            return loaded;
        }
        float factor = (float) MAX_SOURCE_SIDE / longest;
        Pixmap scaled = new Pixmap(Math.round(loaded.getWidth() * factor), Math.round(loaded.getHeight() * factor),
                Pixmap.Format.RGBA8888);
        scaled.setFilter(Pixmap.Filter.BiLinear);
        scaled.drawPixmap(loaded, 0, 0, loaded.getWidth(), loaded.getHeight(), 0, 0, scaled.getWidth(),
                scaled.getHeight());
        loaded.dispose();
        return scaled;
    }

    private void zoom(float factor) {
        if (frame != null) {
            frame.zoom(factor);
            layoutPhoto();
        }
    }

    private void layoutPhoto() {
        float w = frame.shownWidth();
        float h = frame.shownHeight();
        photoImage.setBounds(FRAME_CENTER_X + frame.offsetX() - w / 2f, FRAME_CENTER_Y + frame.offsetY() - h / 2f, w, h);
    }

    private void usePhoto() {
        if (frame == null) {
            return;
        }
        PhotoFrame.Crop crop = frame.crop();
        Path folder = game.saveFolder();
        Pixmap out = new Pixmap(OUTPUT_SIZE, OUTPUT_SIZE, Pixmap.Format.RGBA8888);
        try {
            Files.createDirectories(folder);
            out.setFilter(Pixmap.Filter.BiLinear);
            out.drawPixmap(source, crop.x(), crop.y(), crop.size(), crop.size(), 0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
            roundCorners(out);
            PixmapIO.writePNG(Gdx.files.absolute(folder.resolve(AccountPictures.PHOTO_FILE).toString()), out);
        } catch (IOException | GdxRuntimeException e) {
            LOG.log(Level.WARNING, "Could not save the photo", e);
            showMessage(AccountStrings.SAVE_FAILED, 3f);
            return;
        } finally {
            out.dispose();
        }
        ui.pictures().photoChanged(folder);
        ui.accounts().changePicture(game.account().id(), new Picture(Picture.Kind.PHOTO, ""));
        back();
    }

    /** Makes everything outside the circle transparent, so the photo looks round everywhere. */
    private static void roundCorners(Pixmap pixmap) {
        pixmap.setBlending(Pixmap.Blending.None);
        float r = OUTPUT_SIZE / 2f;
        for (int y = 0; y < OUTPUT_SIZE; y++) {
            for (int x = 0; x < OUTPUT_SIZE; x++) {
                float dx = x + 0.5f - r;
                float dy = y + 0.5f - r;
                if (dx * dx + dy * dy > r * r) {
                    pixmap.drawPixel(x, y, 0);
                }
            }
        }
    }

    private void back() {
        release();
        game.nav().back();
    }

    /** A dark layer over the whole screen with a round hole for the frame. */
    private static Texture shadeWithHole() {
        Pixmap pixmap = new Pixmap((int) UiConfig.VIEW_WIDTH, (int) UiConfig.VIEW_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(SHADE);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        // Pixmap y goes down.
        pixmap.fillCircle((int) FRAME_CENTER_X, (int) (UiConfig.VIEW_HEIGHT - FRAME_CENTER_Y), (int) (FRAME / 2f));
        Texture result = new Texture(pixmap);
        pixmap.dispose();
        return result;
    }

    private void release() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        if (source != null) {
            source.dispose();
            source = null;
        }
        photoImage.setDrawable(null);
        frame = null;
    }

    @Override
    public void dispose() {
        release();
        shade.dispose();
        super.dispose();
    }
}
