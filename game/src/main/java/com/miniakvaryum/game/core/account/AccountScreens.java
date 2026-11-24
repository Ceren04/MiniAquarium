package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.miniakvaryum.domain.core.account.AccountService;
import com.miniakvaryum.game.core.PhotoSource;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.Navigator;
import java.util.Objects;

/**
 * The account screens inside the game: edit profile, choose a picture, adjust a photo, change
 * the safety color, and the sign-out question. The profile feature opens them by id with
 * {@code ui.nav().show(...)}; "Back" returns with {@link Navigator#back()}.
 */
public final class AccountScreens {

    /** Names, password, and the buttons to the other screens. */
    public static final String EDIT_PROFILE = "account-edit";
    /** Avatars, gallery, first letter. */
    public static final String PICTURE = "account-picture";
    /** "Do you really want to sign out?" */
    public static final String SIGN_OUT = SignOutConfirmScreen.ID;

    static final String SAFETY_COLOR = "account-safety-color";
    static final String ADJUST_PHOTO = "account-adjust-photo";

    private AccountScreens() {
    }

    /** Registers the screens for the session of {@code ui}. */
    public static void register(UiContext ui, PhotoSource photos) {
        Objects.requireNonNull(photos, "photos");
        InGame context = new InGame(ui);
        Navigator nav = ui.nav();
        PhotoAdjustScreen[] adjust = new PhotoAdjustScreen[1];
        nav.register(EDIT_PROFILE, () -> new EditProfileScreen(context, ui));
        nav.register(SAFETY_COLOR, () -> new ChangeSafetyColorScreen(context, ui));
        nav.register(ADJUST_PHOTO, () -> adjust[0] = new PhotoAdjustScreen(context, ui));
        nav.register(PICTURE, () -> new PictureChooserScreen(context, ui, photos, file -> {
            nav.show(ADJUST_PHOTO);
            adjust[0].open(file);
        }));
        nav.register(SIGN_OUT, () -> new SignOutConfirmScreen(ui.skin(), ui::signOut, nav::back));
    }

    /** {@link AccountUi} for screens inside a running game. */
    private static final class InGame implements AccountUi {
        private static final Color BACKGROUND = new Color(0.05f, 0.24f, 0.38f, 1f);

        private final UiContext ui;
        private TextField.TextFieldStyle fieldStyle;

        InGame(UiContext ui) {
            this.ui = ui;
        }

        @Override
        public Skin skin() {
            return ui.skin();
        }

        @Override
        public AccountService accounts() {
            return ui.accounts();
        }

        @Override
        public AccountPictures pictures() {
            return ui.pictures();
        }

        @Override
        public TextField.TextFieldStyle fieldStyle() {
            if (fieldStyle == null) {
                fieldStyle = AccountScreen.paddedFieldStyle(ui.skin());
            }
            return fieldStyle;
        }

        @Override
        public Color background() {
            return BACKGROUND;
        }
    }
}
