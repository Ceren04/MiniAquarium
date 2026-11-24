package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.miniakvaryum.domain.core.account.Picture;
import com.miniakvaryum.game.core.ui.UiConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Sign-up step 3: a big preview on the left, the avatar rows on the right, and "Skip" (first
 * letter instead of a picture). Camera and gallery come with the profile step.
 */
final class PictureScreen extends FlowScreen {

    private static final float PREVIEW = 220f;
    private static final float AVATAR = 96f;

    private final Container<Actor> preview = new Container<>();
    private final List<Choice> choices = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();

    PictureScreen(AccountFlow flow) {
        super(flow);
        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.PICTURE_TITLE)).colspan(2).padBottom(UiConfig.GAP / 2f).row();
        root.add(small(AccountStrings.PICTURE_TEXT, HINT)).colspan(2).padBottom(UiConfig.GAP * 2).row();

        preview.size(PREVIEW);
        root.add(preview).size(PREVIEW).top().padRight(UiConfig.GAP * 5).padTop(UiConfig.GAP * 4);

        Table rows = new Table();
        for (AvatarCatalog.Row row : AvatarCatalog.ROWS) {
            rows.add(small(row.title(), HINT)).left().row();
            Table line = new Table();
            for (String id : row.ids()) {
                Choice choice = new Choice(ring((int) AVATAR + 8), picture(Picture.avatar(id), "", AVATAR),
                        () -> pick(Picture.avatar(id)));
                choices.add(choice);
                ids.add(id);
                line.add(choice).size(AVATAR + 8).pad(2);
            }
            rows.add(line).left().padBottom(UiConfig.GAP).row();
        }
        root.add(rows).top().row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, () -> flow.show(AccountFlow.SAFETY_COLOR)))
                .size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add(small(String.format(AccountStrings.STEP, 3), HINT)).expandX();
        buttons.add(button(AccountStrings.SKIP, () -> {
            pick(Picture.initial());
            flow.show(AccountFlow.ALL_SET);
        })).size(WIDE_BUTTON * 0.7f, UiConfig.MIN_TOUCH_SIZE).padRight(UiConfig.GAP);
        buttons.add(button(AccountStrings.NEXT, () -> flow.show(AccountFlow.ALL_SET)))
                .size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons).colspan(2).growX().expandY().bottom();
    }

    private void pick(Picture picture) {
        flow.setDraft(flow.draft().withPicture(picture));
        refresh();
    }

    @Override
    protected void onShow() {
        refresh();
    }

    private void refresh() {
        Picture picture = flow.draft().picture();
        for (int i = 0; i < choices.size(); i++) {
            choices.get(i).setSelected(picture.kind() == Picture.Kind.AVATAR && ids.get(i).equals(picture.avatarId()));
        }
        preview.setActor(picture(picture, flow.draft().firstName(), PREVIEW));
        preview.fill();
    }
}
