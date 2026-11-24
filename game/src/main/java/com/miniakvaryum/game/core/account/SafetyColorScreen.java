package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.domain.core.account.SafetyColor;
import com.miniakvaryum.game.core.ui.UiConfig;

/** Sign-up step 2: the child picks a favorite color, asked again after "Forgot password?". */
final class SafetyColorScreen extends FlowScreen {

    private final Choice[] choices = new Choice[SafetyColor.values().length];
    private final TextButton next;

    SafetyColorScreen(AccountFlow flow) {
        super(flow);
        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.SAFETY_TITLE)).padBottom(UiConfig.GAP).row();
        root.add(text(AccountStrings.SAFETY_TEXT)).padBottom(UiConfig.GAP * 3).row();
        root.add(colorGrid(choices, this::pick)).row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, () -> flow.show(AccountFlow.ABOUT_YOU)))
                .size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add(small(String.format(AccountStrings.STEP, 2), HINT)).expandX();
        next = button(AccountStrings.NEXT, () -> flow.show(AccountFlow.PICTURE));
        buttons.add(next).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons).growX().expandY().bottom();
    }

    private void pick(SafetyColor color) {
        flow.setDraft(flow.draft().withSafetyColor(color));
        refresh();
    }

    @Override
    protected void onShow() {
        refresh();
    }

    private void refresh() {
        SafetyColor picked = flow.draft().safetyColor();
        select(choices, picked == null ? -1 : picked.ordinal());
        setEnabled(next, picked != null);
    }
}
