package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.SignInResult;
import com.miniakvaryum.game.core.ui.UiConfig;

/**
 * Sign in: the accounts of this device as pictures (a tap fills in the username), username,
 * password, "Remember me", and "Forgot password?".
 */
final class SignInScreen extends AccountScreen {

    private static final float PICTURE = 84f;

    private final Table accountRow = new Table();
    private final TextField username;
    private final TextField password;
    private final TextButton showPassword;
    private final TextButton remember;
    private final Label status;
    /** Remaining lock time counted down with the frame time; 0 when not locked. */
    private float lockSecondsLeft;

    SignInScreen(AccountFlow flow) {
        super(flow);
        username = field(flow.accounts().config().usernameMaxLength(), false, this::clearStatus);
        password = field(flow.accounts().config().passwordMaxLength(), true, this::clearStatus);
        showPassword = showHide(password);
        remember = rememberToggle();
        status = small("", ERROR);

        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.SIGN_IN)).padBottom(UiConfig.GAP).row();
        root.add(small(AccountStrings.SIGN_IN_ACCOUNTS, HINT)).padBottom(UiConfig.GAP / 2f).row();
        ScrollPane scroll = new ScrollPane(accountRow);
        scroll.setScrollingDisabled(false, true);
        root.add(scroll).height(PICTURE + 40).maxWidth(UiConfig.VIEW_WIDTH - 80).padBottom(UiConfig.GAP * 2).row();

        Table form = new Table();
        form.add(small(AccountStrings.USERNAME, HINT)).left().row();
        form.add(username).size(FIELD_WIDTH, FIELD_HEIGHT).left().padBottom(UiConfig.GAP).row();
        form.add(small(AccountStrings.PASSWORD, HINT)).left().row();
        Table passwordRow = new Table();
        passwordRow.add(password).size(FIELD_WIDTH, FIELD_HEIGHT);
        passwordRow.add(showPassword).size(100, FIELD_HEIGHT).padLeft(10);
        form.add(passwordRow).left().row();
        form.add(status).left().height(30).row();
        form.add(remember).size(WIDE_BUTTON * 1.5f, UiConfig.MIN_TOUCH_SIZE).left().padBottom(UiConfig.GAP).row();
        root.add(form).row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, () -> flow.show(AccountFlow.WELCOME)))
                .size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add(button(AccountStrings.FORGOT_PASSWORD, () -> {
            flow.setTypedUsername(username.getText());
            flow.show(AccountFlow.FORGOT_PASSWORD);
        })).size(WIDE_BUTTON * 1.4f, UiConfig.MIN_TOUCH_SIZE).expandX();
        buttons.add(button(AccountStrings.SIGN_IN, this::signIn)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons).growX().expandY().bottom();

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                    signIn();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onShow() {
        accountRow.clearChildren();
        for (Account account : flow.accounts().all()) {
            Table cell = new Table();
            Choice choice = new Choice(ring((int) PICTURE + 8), picture(account.picture(), account.firstName(), PICTURE),
                    () -> {
                        username.setText(account.username());
                        password.setText("");
                        stage.setKeyboardFocus(password);
                        clearStatus();
                    });
            cell.add(choice).size(PICTURE + 8).row();
            cell.add(small(account.username(), HINT));
            accountRow.add(cell).padRight(UiConfig.GAP);
        }
        if (flow.accounts().all().isEmpty()) {
            accountRow.add(small(AccountStrings.NO_ACCOUNTS, HINT));
        }
        username.setText(flow.accounts().all().size() == 1 ? flow.accounts().all().get(0).username() : "");
        password.setText("");
        hide(showPassword, password);
        setRemember(remember, false);
        lockSecondsLeft = 0;
        clearStatus();
        stage.setKeyboardFocus(username.getText().isEmpty() ? username : password);
    }

    @Override
    protected void update(float delta) {
        if (lockSecondsLeft > 0) {
            lockSecondsLeft -= delta;
            status.setText(lockSecondsLeft > 0 ? lockText((long) (lockSecondsLeft * 1000)) : "");
        }
    }

    private void clearStatus() {
        if (lockSecondsLeft <= 0) {
            status.setText("");
        }
    }

    private void signIn() {
        SignInResult result = flow.accounts().signIn(username.getText().trim(), password.getText());
        switch (result.status()) {
            case OK -> flow.finish(result.account().orElseThrow(), remember.isChecked());
            case WRONG -> {
                status.setText(AccountStrings.WRONG_SIGN_IN);
                password.setText("");
                stage.setKeyboardFocus(password);
            }
            case LOCKED -> {
                lockSecondsLeft = result.lockedRemainingMillis() / 1000f;
                status.setText(lockText(result.lockedRemainingMillis()));
                password.setText("");
            }
        }
    }
}
