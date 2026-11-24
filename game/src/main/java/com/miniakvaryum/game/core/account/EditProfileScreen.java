package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.AccountsConfig;
import com.miniakvaryum.domain.core.account.EditResult;
import com.miniakvaryum.domain.core.account.SignUpProblem;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.UiConfig;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * "Edit profile": picture (opens the picture screen), first and last name, password (needs the
 * current one), and the button to change the safety color. Username and birthday cannot be
 * changed; the screen says so.
 */
final class EditProfileScreen extends AccountScreen {

    private static final float PICTURE = 140f;
    private static final DateTimeFormatter BIRTHDAY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final UiContext game;
    private final Container<Actor> picture = new Container<>();
    private final TextField firstName;
    private final TextField lastName;
    private final TextField current;
    private final TextField password;
    private final TextField repeat;
    private final TextButton showPassword;
    private final TextButton saveNames;
    private final TextButton changePassword;
    private final Label namesStatus;
    private final Label passwordStatus;
    private final Label fixed;

    EditProfileScreen(AccountUi accountUi, UiContext game) {
        super(accountUi);
        this.game = game;
        AccountsConfig config = ui.accounts().config();
        firstName = field(config.nameMaxLength(), false, this::refresh);
        lastName = field(config.nameMaxLength(), false, this::refresh);
        current = field(config.passwordMaxLength(), true, this::refresh);
        password = field(config.passwordMaxLength(), true, this::refresh);
        repeat = field(config.passwordMaxLength(), true, this::refresh);
        showPassword = showHide(current, password, repeat);
        namesStatus = small("", HINT);
        passwordStatus = small("", HINT);
        fixed = small("", HINT);
        saveNames = button(AccountStrings.SAVE_NAMES, this::saveNames);
        changePassword = button(AccountStrings.CHANGE_PASSWORD, this::changePassword);

        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.EDIT_PROFILE)).colspan(2).padBottom(UiConfig.GAP).row();

        Table left = new Table();
        Table pictureRow = new Table();
        picture.size(PICTURE).fill();
        pictureRow.add(picture).size(PICTURE).padRight(UiConfig.GAP * 2);
        pictureRow.add(button(AccountStrings.CHANGE_PICTURE, () -> game.nav().show(AccountScreens.PICTURE)))
                .size(WIDE_BUTTON * 1.1f, UiConfig.MIN_TOUCH_SIZE);
        left.add(pictureRow).left().padBottom(UiConfig.GAP * 2).row();
        left.add(small(AccountStrings.FIRST_NAME, HINT)).left().row();
        left.add(firstName).size(FIELD_WIDTH, FIELD_HEIGHT).left().padBottom(UiConfig.GAP).row();
        left.add(small(AccountStrings.LAST_NAME, HINT)).left().row();
        left.add(lastName).size(FIELD_WIDTH, FIELD_HEIGHT).left().padBottom(UiConfig.GAP).row();
        left.add(saveNames).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE).left().row();
        left.add(namesStatus).left().height(30).row();

        Table right = new Table();
        right.add(small(AccountStrings.CURRENT_PASSWORD, HINT)).left().row();
        Table currentRow = new Table();
        currentRow.add(current).size(FIELD_WIDTH - 110, FIELD_HEIGHT);
        currentRow.add(showPassword).size(100, FIELD_HEIGHT).padLeft(10);
        right.add(currentRow).left().padBottom(UiConfig.GAP).row();
        right.add(small(AccountStrings.NEW_PASSWORD, HINT)).left().row();
        right.add(password).size(FIELD_WIDTH, FIELD_HEIGHT).left().padBottom(UiConfig.GAP).row();
        right.add(small(AccountStrings.REPEAT_PASSWORD, HINT)).left().row();
        right.add(repeat).size(FIELD_WIDTH, FIELD_HEIGHT).left().padBottom(UiConfig.GAP).row();
        right.add(changePassword).size(WIDE_BUTTON * 1.2f, UiConfig.MIN_TOUCH_SIZE).left().row();
        right.add(passwordStatus).left().height(30).padBottom(UiConfig.GAP).row();
        right.add(button(AccountStrings.CHANGE_SAFETY_COLOR, () -> game.nav().show(AccountScreens.SAFETY_COLOR)))
                .size(WIDE_BUTTON * 1.5f, UiConfig.MIN_TOUCH_SIZE).left();

        root.add(left).top().padRight(UiConfig.GAP * 6);
        root.add(right).top().row();
        root.add(fixed).colspan(2).padTop(UiConfig.GAP).row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, game.nav()::back)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add().expandX();
        root.add(buttons).colspan(2).growX().expandY().bottom();
    }

    @Override
    protected void onShow() {
        Account account = game.account();
        picture.setActor(ui.pictures().of(account, game.saveFolder(), PICTURE));
        firstName.setText(account.firstName());
        lastName.setText(account.lastName());
        current.setText("");
        password.setText("");
        repeat.setText("");
        hide(showPassword, current, password, repeat);
        namesStatus.setText("");
        passwordStatus.setText("");
        fixed.setText(String.format(AccountStrings.NOT_CHANGEABLE, account.username(),
                BIRTHDAY.format(account.birthday())));
        refresh();
    }

    private void refresh() {
        Account account = game.account();
        boolean namesChanged = !firstName.getText().trim().equals(account.firstName())
                || !lastName.getText().trim().equals(account.lastName());
        boolean namesValid = !firstName.getText().isBlank() && !lastName.getText().isBlank();
        setEnabled(saveNames, namesChanged && namesValid);
        Optional<SignUpProblem> problem = ui.accounts().passwordProblem(password.getText(), repeat.getText());
        setEnabled(changePassword, !current.getText().isEmpty() && problem.isEmpty());
        if (!password.getText().isEmpty() && problem.isPresent()) {
            passwordStatus.setColor(ERROR);
            passwordStatus.setText(switch (problem.get()) {
                case PASSWORD_TOO_SHORT -> AccountStrings.ERROR_PASSWORD_SHORT;
                case PASSWORD_TOO_LONG -> AccountStrings.ERROR_PASSWORD_LONG;
                default -> repeat.getText().isEmpty() ? "" : AccountStrings.ERROR_PASSWORDS_DIFFER;
            });
        } else if (passwordStatus.getColor().equals(ERROR)) {
            passwordStatus.setText("");
        }
    }

    private void saveNames() {
        EditResult result = ui.accounts().changeNames(game.account().id(), firstName.getText(), lastName.getText());
        namesStatus.setColor(result == EditResult.OK ? HINT : ERROR);
        namesStatus.setText(result == EditResult.OK ? AccountStrings.NAMES_SAVED : AccountStrings.ERROR_FIRST_NAME);
        refresh();
    }

    private void changePassword() {
        EditResult result = ui.accounts().changePassword(game.account().id(), current.getText(), password.getText(),
                repeat.getText());
        if (result == EditResult.OK) {
            current.setText("");
            password.setText("");
            repeat.setText("");
            passwordStatus.setColor(HINT);
            passwordStatus.setText(AccountStrings.PASSWORD_CHANGED);
        } else {
            passwordStatus.setColor(ERROR);
            passwordStatus.setText(result == EditResult.WRONG_PASSWORD ? AccountStrings.WRONG_PASSWORD
                    : AccountStrings.ERROR_PASSWORD_SHORT);
        }
        refresh();
    }
}
