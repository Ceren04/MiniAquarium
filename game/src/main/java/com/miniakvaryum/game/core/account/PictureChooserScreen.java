package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.Picture;
import com.miniakvaryum.game.core.PhotoSource;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.UiConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Picture of the signed-in account: a tap on an avatar saves it at once; "Choose from gallery"
 * opens a file window and then "Adjust your photo"; "Use first letter" removes the picture.
 * A camera button comes once the desktop webcam test (plan V12, step 0) has passed.
 */
final class PictureChooserScreen extends AccountScreen {

    private static final float PREVIEW = 200f;
    private static final float AVATAR = 90f;

    private final UiContext game;
    private final PhotoSource photos;
    private final Consumer<Path> onPhoto;
    private final Container<Actor> preview = new Container<>();
    private final List<Choice> choices = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();

    /**
     * @param onPhoto called with the picked file; opens "Adjust your photo"
     */
    PictureChooserScreen(AccountUi accountUi, UiContext game, PhotoSource photos, Consumer<Path> onPhoto) {
        super(accountUi);
        this.game = game;
        this.photos = photos;
        this.onPhoto = onPhoto;

        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.PICTURE_TITLE)).colspan(2).padBottom(UiConfig.GAP * 2).row();

        Table left = new Table();
        preview.size(PREVIEW).fill();
        left.add(preview).size(PREVIEW).padBottom(UiConfig.GAP * 2).row();
        if (photos.hasGallery()) {
            left.add(button(AccountStrings.CHOOSE_FROM_GALLERY, this::gallery))
                    .size(WIDE_BUTTON * 1.4f, UiConfig.MIN_TOUCH_SIZE).padBottom(UiConfig.GAP).row();
        }
        left.add(button(AccountStrings.USE_FIRST_LETTER, () -> save(Picture.initial())))
                .size(WIDE_BUTTON * 1.4f, UiConfig.MIN_TOUCH_SIZE);
        root.add(left).top().padRight(UiConfig.GAP * 5);

        Table rows = new Table();
        for (AvatarCatalog.Row row : AvatarCatalog.ROWS) {
            rows.add(small(row.title(), HINT)).left().row();
            Table line = new Table();
            for (String id : row.ids()) {
                Choice choice = new Choice(ring((int) AVATAR + 8), picture(Picture.avatar(id), "", AVATAR),
                        () -> save(Picture.avatar(id)));
                choices.add(choice);
                ids.add(id);
                line.add(choice).size(AVATAR + 8).pad(2);
            }
            rows.add(line).left().padBottom(UiConfig.GAP).row();
        }
        root.add(rows).top().row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.DONE, game.nav()::back)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add().expandX();
        root.add(buttons).colspan(2).growX().expandY().bottom();
    }

    @Override
    protected void onShow() {
        refresh();
    }

    private void save(Picture picture) {
        ui.accounts().changePicture(game.account().id(), picture);
        refresh();
    }

    private void gallery() {
        Optional<Path> file = photos.pickFromGallery();
        file.ifPresent(onPhoto);
    }

    private void refresh() {
        Account account = game.account();
        Picture picture = account.picture();
        for (int i = 0; i < choices.size(); i++) {
            choices.get(i).setSelected(picture.kind() == Picture.Kind.AVATAR && ids.get(i).equals(picture.avatarId()));
        }
        preview.setActor(ui.pictures().of(account, game.saveFolder(), PREVIEW));
    }
}
