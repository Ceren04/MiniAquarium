package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.game.core.CoreStrings;
import com.miniakvaryum.game.core.ui.UiConfig;

/** First screen without a remembered account: "Sign up" and "Sign in". */
final class WelcomeScreen extends AccountScreen {

    private final TextButton signIn;

    WelcomeScreen(AccountFlow flow) {
        super(flow);
        Table root = newRootTable();
        root.add(title(CoreStrings.APP_TITLE)).padBottom(UiConfig.GAP).row();
        root.add(text(AccountStrings.WELCOME_SUBTITLE)).padBottom(UiConfig.GAP * 6).row();
        root.add(button(AccountStrings.SIGN_UP, flow::startSignUp))
                .size(WIDE_BUTTON * 1.5f, UiConfig.MIN_TOUCH_SIZE * 1.2f).padBottom(UiConfig.GAP * 2).row();
        signIn = button(AccountStrings.SIGN_IN, () -> flow.show(AccountFlow.SIGN_IN));
        root.add(signIn).size(WIDE_BUTTON * 1.5f, UiConfig.MIN_TOUCH_SIZE * 1.2f);
    }

    @Override
    protected void onShow() {
        setEnabled(signIn, !flow.accounts().all().isEmpty());
    }
}
