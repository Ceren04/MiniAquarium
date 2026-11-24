package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.ResetResult;
import com.miniakvaryum.domain.core.account.SafetyColor;
import com.miniakvaryum.domain.core.account.SignUpProblem;
import com.miniakvaryum.game.core.ui.UiConfig;
import java.util.Optional;

/**
 * "Forgot password?" in three steps on one screen: pick the account, pick the safety color,
 * type a new password twice. A right color opens the account's aquarium after saving.
 */
final class ForgotPasswordScreen extends AccountScreen {

    private static final float PICTURE = 96f;

    private enum Step { ACCOUNT, COLOR, PASSWORD }

    private final Label heading;
    private final Table body = new Table();
    private final Table accountRow = new Table();
    private final Table colorStep;
    private final Table passwordStep;
    private final Choice[] colors = new Choice[SafetyColor.values().length];
    private final TextField password;
    private final TextField repeat;
    private final TextButton showPassword;
    private final TextButton save;
    private final Label status;

    private Step step = Step.ACCOUNT;
    private Account account;
    private SafetyColor color;
    private float lockSecondsLeft;

    ForgotPasswordScreen(AccountFlow flow) {
        super(flow);
        heading = title("");
        status = small("", ERROR);
        password = field(flow.accounts().config().passwordMaxLength(), true, this::refreshPassword);
        repeat = field(flow.accounts().config().passwordMaxLength(), true, this::refreshPassword);
        showPassword = showHide(password, repeat);
        save = button(AccountStrings.SAVE, this::savePassword);

        colorStep = new Table();
        colorStep.add(colorGrid(colors, this::pickColor));

        passwordStep = new Table();
        passwordStep.add(small(AccountStrings.NEW_PASSWORD, HINT)).left().row();
        Table passwordRow = new Table();
        passwordRow.add(password).size(FIELD_WIDTH, FIELD_HEIGHT);
        passwordRow.add(showPassword).size(100, FIELD_HEIGHT).padLeft(10);
        passwordStep.add(passwordRow).left().padBottom(UiConfig.GAP).row();
        passwordStep.add(small(AccountStrings.REPEAT_PASSWORD, HINT)).left().row();
        passwordStep.add(repeat).size(FIELD_WIDTH, FIELD_HEIGHT).left().padBottom(UiConfig.GAP * 2).row();
        passwordStep.add(save).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE).left();

        Table root = newRootTable();
        root.top();
        root.add(heading).padBottom(UiConfig.GAP * 2).row();
        root.add(body).row();
        root.add(status).height(36).padTop(UiConfig.GAP).row();
        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, this::back)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add().expandX();
        root.add(buttons).growX().expandY().bottom();
    }

    @Override
    protected void onShow() {
        lockSecondsLeft = 0;
        account = null;
        color = null;
        Optional<Account> typed = flow.accounts().findByUsername(flow.typedUsername().trim());
        if (typed.isPresent()) {
            account = typed.get();
            show(Step.COLOR);
        } else if (flow.accounts().all().size() == 1) {
            account = flow.accounts().all().get(0);
            show(Step.COLOR);
        } else {
            show(Step.ACCOUNT);
        }
    }

    @Override
    protected void update(float delta) {
        if (lockSecondsLeft > 0) {
            lockSecondsLeft -= delta;
            status.setText(lockSecondsLeft > 0 ? lockText((long) (lockSecondsLeft * 1000)) : "");
        }
    }

    private void show(Step next) {
        step = next;
        body.clearChildren();
        status.setText("");
        switch (next) {
            case ACCOUNT -> {
                heading.setText(AccountStrings.WHO_ARE_YOU);
                fillAccounts();
                ScrollPane scroll = new ScrollPane(accountRow);
                scroll.setScrollingDisabled(false, true);
                body.add(scroll).height(PICTURE + 44).maxWidth(UiConfig.VIEW_WIDTH - 80);
            }
            case COLOR -> {
                heading.setText(AccountStrings.WHAT_COLOR);
                select(colors, -1);
                Table who = new Table();
                who.add(picture(account.picture(), account.firstName(), 56)).size(56).padRight(UiConfig.GAP);
                who.add(text(account.username()));
                body.add(who).padBottom(UiConfig.GAP * 2).row();
                body.add(colorStep);
            }
            case PASSWORD -> {
                heading.setText(AccountStrings.NEW_PASSWORD_TITLE);
                password.setText("");
                repeat.setText("");
                hide(showPassword, password, repeat);
                body.add(passwordStep);
                stage.setKeyboardFocus(password);
                refreshPassword();
            }
        }
    }

    private void fillAccounts() {
        accountRow.clearChildren();
        for (Account each : flow.accounts().all()) {
            Table cell = new Table();
            cell.add(new Choice(ring((int) PICTURE + 8), picture(each.picture(), each.firstName(), PICTURE), () -> {
                account = each;
                show(Step.COLOR);
            })).size(PICTURE + 8).row();
            cell.add(small(each.username(), HINT));
            accountRow.add(cell).padRight(UiConfig.GAP);
        }
    }

    private void pickColor(SafetyColor picked) {
        if (lockSecondsLeft > 0) {
            return;
        }
        ResetResult result = flow.accounts().checkSafetyColor(account.id(), picked);
        switch (result.status()) {
            case OK -> {
                color = picked;
                show(Step.PASSWORD);
            }
            case WRONG_COLOR -> status.setText(AccountStrings.WRONG_COLOR);
            case LOCKED -> {
                lockSecondsLeft = result.lockedRemainingMillis() / 1000f;
                status.setText(lockText(result.lockedRemainingMillis()));
            }
            case INVALID_PASSWORD -> throw new IllegalStateException("checkSafetyColor does not check passwords");
        }
    }

    private void refreshPassword() {
        Optional<SignUpProblem> problem = flow.accounts().passwordProblem(password.getText(), repeat.getText());
        String text = "";
        if (problem.isPresent() && !password.getText().isEmpty()) {
            text = switch (problem.get()) {
                case PASSWORD_TOO_SHORT -> AccountStrings.ERROR_PASSWORD_SHORT;
                case PASSWORD_TOO_LONG -> AccountStrings.ERROR_PASSWORD_LONG;
                default -> repeat.getText().isEmpty() ? "" : AccountStrings.ERROR_PASSWORDS_DIFFER;
            };
        }
        status.setText(text);
        setEnabled(save, problem.isEmpty());
    }

    private void savePassword() {
        ResetResult result = flow.accounts().resetPassword(account.id(), color, password.getText(), repeat.getText());
        if (result.status() == ResetResult.Status.OK) {
            flow.finish(result.account().orElseThrow(), false);
        } else {
            refreshPassword();
        }
    }

    private void back() {
        if (step == Step.ACCOUNT || flow.accounts().all().size() == 1 && step == Step.COLOR) {
            flow.show(AccountFlow.SIGN_IN);
        } else if (step == Step.COLOR) {
            show(Step.ACCOUNT);
        } else {
            show(Step.COLOR);
        }
    }
}
