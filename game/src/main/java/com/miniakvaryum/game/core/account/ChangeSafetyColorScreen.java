package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.miniakvaryum.domain.core.account.EditResult;
import com.miniakvaryum.domain.core.account.SafetyColor;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.UiConfig;

/** Changes the safety color; the current password is needed. */
final class ChangeSafetyColorScreen extends AccountScreen {

    private final UiContext game;
    private final TextField current;
    private final Choice[] choices = new Choice[SafetyColor.values().length];
    private final TextButton save;
    private final Label status;
    private SafetyColor picked;

    ChangeSafetyColorScreen(AccountUi accountUi, UiContext game) {
        super(accountUi);
        this.game = game;
        current = field(ui.accounts().config().passwordMaxLength(), true, this::refresh);
        save = button(AccountStrings.SAVE, this::save);
        status = small("", HINT);

        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.CHANGE_SAFETY_COLOR)).padBottom(UiConfig.GAP / 2f).row();
        root.add(small(AccountStrings.PICK_NEW_COLOR, HINT)).padBottom(UiConfig.GAP).row();
        Table passwordRow = new Table();
        passwordRow.add(small(AccountStrings.CURRENT_PASSWORD, HINT)).padRight(UiConfig.GAP);
        passwordRow.add(current).size(FIELD_WIDTH, FIELD_HEIGHT);
        root.add(passwordRow).padBottom(UiConfig.GAP).row();
        root.add(colorGrid(choices, this::pick)).row();
        root.add(status).height(30).row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, game.nav()::back)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add().expandX();
        buttons.add(save).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons).growX().expandY().bottom();
    }

    @Override
    protected void onShow() {
        picked = null;
        current.setText("");
        status.setText("");
        select(choices, -1);
        stage.setKeyboardFocus(current);
        refresh();
    }

    private void pick(SafetyColor color) {
        picked = color;
        select(choices, color.ordinal());
        refresh();
    }

    private void refresh() {
        setEnabled(save, picked != null && !current.getText().isEmpty());
    }

    private void save() {
        EditResult result = ui.accounts().changeSafetyColor(game.account().id(), current.getText(), picked);
        if (result == EditResult.OK) {
            game.nav().back();
            return;
        }
        status.setColor(ERROR);
        status.setText(AccountStrings.WRONG_PASSWORD);
        current.setText("");
        refresh();
    }
}
