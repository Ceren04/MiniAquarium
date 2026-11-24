package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Scaling;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.Picture;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Draws profile pictures: an avatar from {@code assets/avatars}, the account's photo
 * ({@code photo.png} in its folder), or the first letter of the first name in a colored circle.
 * Every texture is loaded once and kept until {@link #dispose()}; the game keeps one instance for
 * its whole life.
 */
public final class AccountPictures implements Disposable {

    /** Name of the photo file in the account folder. */
    public static final String PHOTO_FILE = "photo.png";

    private static final Logger LOG = Logger.getLogger(AccountPictures.class.getName());

    /** Circle colors for the first letter when no picture was chosen. */
    private static final Color[] INITIAL_COLORS = {
        Color.valueOf("FF9F2E"), Color.valueOf("8ACBEE"), Color.valueOf("F2B84B"), Color.valueOf("43A047"),
        Color.valueOf("8E24AA"), Color.valueOf("F06292"),
    };

    private final Skin skin;
    private final ShapeTextures shapes = new ShapeTextures();
    private final Map<String, Texture> cache = new HashMap<>();
    private final Map<Path, Texture> photos = new HashMap<>();
    private final List<Texture> retired = new ArrayList<>();
    private int photoVersion;

    public AccountPictures(Skin skin) {
        this.skin = Objects.requireNonNull(skin, "skin");
    }

    /** The picture of an account, {@code size} pixels wide and high. */
    public Actor of(Account account, Path accountFolder, float size) {
        return of(account.picture(), account.firstName(), accountFolder, size);
    }

    /**
     * A picture, {@code size} pixels wide and high.
     *
     * @param accountFolder where {@code photo.png} lies; only used for {@link Picture.Kind#PHOTO}
     */
    public Actor of(Picture picture, String firstName, Path accountFolder, float size) {
        Texture texture = switch (picture.kind()) {
            case AVATAR -> avatar(picture.avatarId());
            case PHOTO -> photo(accountFolder);
            case INITIAL -> null;
        };
        if (texture != null) {
            Image image = new Image(texture);
            image.setScaling(Scaling.fit);
            image.setTouchable(Touchable.disabled);
            return image;
        }
        return initial(firstName, size);
    }

    /** The first letter of the name in a colored circle. */
    private Actor initial(String firstName, float size) {
        String name = firstName == null ? "" : firstName.trim();
        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        Color color = INITIAL_COLORS[Math.floorMod(name.hashCode(), INITIAL_COLORS.length)];
        Image circle = new Image(circle((int) size, color));
        circle.setScaling(Scaling.fit);
        Label label = new Label(letter, skin, UiSkin.TITLE);
        label.setAlignment(Align.center);
        label.setFontScale(size / 100f);
        Stack stack = new Stack();
        stack.add(circle);
        stack.add(label);
        stack.setTouchable(Touchable.disabled);
        return stack;
    }

    /** A filled circle, created once per size and color. */
    public Texture circle(int diameter, Color color) {
        return cache.computeIfAbsent(diameter + "/" + color, key -> shapes.circle(diameter, color));
    }

    /** The image of an avatar, loaded once; a gray circle if the file is missing. */
    public Texture avatar(String avatarId) {
        return cache.computeIfAbsent("avatar:" + avatarId, key -> {
            Texture texture = shapes.fileOr(AvatarCatalog.imagePath(avatarId), () -> shapes.circle(64, Color.GRAY));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        });
    }

    /** The photo of an account; null if it is missing or cannot be read (then the letter is shown). */
    private Texture photo(Path accountFolder) {
        if (accountFolder == null) {
            return null;
        }
        Path file = accountFolder.resolve(PHOTO_FILE);
        Texture texture = photos.get(file);
        if (texture == null && Files.isRegularFile(file)) {
            try {
                texture = new Texture(Gdx.files.absolute(file.toAbsolutePath().toString()));
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                photos.put(file, texture);
            } catch (GdxRuntimeException e) {
                LOG.log(Level.WARNING, "Could not read " + file, e);
            }
        }
        return texture;
    }

    /** Grows by one with every {@link #photoChanged}, so widgets know when to draw again. */
    public int photoVersion() {
        return photoVersion;
    }

    /** Forgets the loaded photo of an account, after a new {@code photo.png} was written. */
    public void photoChanged(Path accountFolder) {
        photoVersion++;
        Texture old = photos.remove(accountFolder.resolve(PHOTO_FILE));
        if (old != null) {
            // A screen may still show it until it is rebuilt; freed in dispose().
            retired.add(old);
        }
    }

    @Override
    public void dispose() {
        photos.values().forEach(Texture::dispose);
        photos.clear();
        retired.forEach(Texture::dispose);
        retired.clear();
        cache.clear();
        shapes.dispose();
    }
}
