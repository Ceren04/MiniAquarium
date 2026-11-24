package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.core.account.AccountsConfig;
import com.miniakvaryum.domain.core.account.SignUpDraft;
import com.miniakvaryum.domain.core.account.SignUpProblem;
import com.miniakvaryum.game.core.ui.UiConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * Sign-up step 1: first name, last name, username, password twice and birthday.
 *
 * <p>Problems are shown under a field as soon as something was typed into it; "Next" stays
 * disabled until all fields are valid. The birthday is picked with arrow buttons, so no
 * keyboard is needed for it.
 */
final class AboutYouScreen extends FlowScreen {

    private static final int MONTHS = 12;

    private final TextField firstName;
    private final TextField lastName;
    private final TextField username;
    private final TextField password;
    private final TextField repeat;
    private final TextButton showPassword;
    private final Label firstNameError;
    private final Label lastNameError;
    private final Label usernameError;
    private final Label passwordError;
    private final Label repeatError;
    private final Label birthdayError;
    private final Label dayValue;
    private final Label monthValue;
    private final Label yearValue;
    private final TextButton next;

    /** Picked birthday parts; 0 = not picked yet. */
    private int day;
    private int month;
    private int year;

    AboutYouScreen(AccountFlow flow) {
        super(flow);
        AccountsConfig config = flow.accounts().config();
        firstName = field(config.nameMaxLength(), false, this::refresh);
        lastName = field(config.nameMaxLength(), false, this::refresh);
        username = field(config.usernameMaxLength(), false, this::refresh);
        password = field(config.passwordMaxLength(), true, this::refresh);
        repeat = field(config.passwordMaxLength(), true, this::refresh);
        showPassword = showHide(password, repeat);
        firstNameError = small("", ERROR);
        lastNameError = small("", ERROR);
        usernameError = small("", ERROR);
        passwordError = small("", ERROR);
        repeatError = small("", ERROR);
        birthdayError = small("", ERROR);
        dayValue = text("");
        monthValue = text("");
        yearValue = text("");

        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.ABOUT_YOU)).colspan(2).padBottom(UiConfig.GAP * 2).row();

        Table left = new Table();
        addField(left, AccountStrings.FIRST_NAME, firstName, firstNameError);
        addField(left, AccountStrings.LAST_NAME, lastName, lastNameError);
        addField(left, AccountStrings.USERNAME, username, usernameError);

        Table right = new Table();
        Table passwordRow = new Table();
        passwordRow.add(password).size(FIELD_WIDTH - 110, FIELD_HEIGHT);
        passwordRow.add(showPassword).size(100, FIELD_HEIGHT).padLeft(10);
        addField(right, AccountStrings.PASSWORD, passwordRow, passwordError);
        addField(right, AccountStrings.REPEAT_PASSWORD, repeat, repeatError);
        right.add(small(AccountStrings.BIRTHDAY, HINT)).left().row();
        right.add(birthdayPicker()).left().row();
        right.add(birthdayError).left().height(28).row();

        root.add(left).top().padRight(UiConfig.GAP * 6);
        root.add(right).top().row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, () -> {
            save();
            flow.show(AccountFlow.WELCOME);
        })).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add(small(String.format(AccountStrings.STEP, 1), HINT)).expandX();
        next = button(AccountStrings.NEXT, () -> {
            save();
            flow.show(AccountFlow.SAFETY_COLOR);
        });
        buttons.add(next).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons).colspan(2).growX().expandY().bottom();
    }

    private void addField(Table column, String label, Actor input, Label error) {
        column.add(small(label, HINT)).left().row();
        column.add(input).size(FIELD_WIDTH, FIELD_HEIGHT).left().row();
        column.add(error).left().height(28).padBottom(UiConfig.GAP / 2f).row();
    }

    /** Day, month and year, each with "<" and ">" buttons. */
    private Table birthdayPicker() {
        Table picker = new Table();
        addSpinner(picker, dayValue, () -> changeDay(-1), () -> changeDay(1), 70);
        addSpinner(picker, monthValue, () -> changeMonth(-1), () -> changeMonth(1), 150);
        addSpinner(picker, yearValue, () -> changeYear(-1), () -> changeYear(1), 90);
        return picker;
    }

    private void addSpinner(Table picker, Label value, Runnable down, Runnable up, float width) {
        picker.add(button("<", down)).size(44, FIELD_HEIGHT);
        value.setAlignment(Align.center);
        picker.add(value).width(width);
        picker.add(button(">", up)).size(44, FIELD_HEIGHT).padRight(UiConfig.GAP);
    }

    private void startBirthdayIfEmpty() {
        if (year == 0) {
            year = flow.accounts().today().getYear() - 8;
            month = 1;
            day = 1;
        }
    }

    private void changeDay(int delta) {
        startBirthdayIfEmpty();
        int length = YearMonth.of(year, month).lengthOfMonth();
        day = Math.floorMod(day - 1 + delta, length) + 1;
        refresh();
    }

    private void changeMonth(int delta) {
        startBirthdayIfEmpty();
        month = Math.floorMod(month - 1 + delta, MONTHS) + 1;
        day = Math.min(day, YearMonth.of(year, month).lengthOfMonth());
        refresh();
    }

    private void changeYear(int delta) {
        startBirthdayIfEmpty();
        int thisYear = flow.accounts().today().getYear();
        year = Math.max(thisYear - 20, Math.min(thisYear, year + delta));
        day = Math.min(day, YearMonth.of(year, month).lengthOfMonth());
        refresh();
    }

    private LocalDate birthday() {
        return year == 0 ? null : LocalDate.of(year, month, day);
    }

    private SignUpDraft current() {
        return flow.draft().withAboutYou(firstName.getText(), lastName.getText(), username.getText(),
                password.getText(), repeat.getText(), birthday());
    }

    private void save() {
        flow.setDraft(current());
    }

    @Override
    protected void onShow() {
        SignUpDraft draft = flow.draft();
        firstName.setText(draft.firstName());
        lastName.setText(draft.lastName());
        username.setText(draft.username());
        password.setText(draft.password());
        repeat.setText(draft.repeatPassword());
        hide(showPassword, password, repeat);
        LocalDate birthday = draft.birthday();
        year = birthday == null ? 0 : birthday.getYear();
        month = birthday == null ? 0 : birthday.getMonthValue();
        day = birthday == null ? 0 : birthday.getDayOfMonth();
        stage.setKeyboardFocus(firstName);
        refresh();
    }

    /** Shows the problems of the fields that are filled in and enables "Next" when all are valid. */
    private void refresh() {
        Set<SignUpProblem> problems = flow.accounts().check(current());
        firstNameError.setText(shown(firstName, problems, SignUpProblem.FIRST_NAME, AccountStrings.ERROR_FIRST_NAME));
        lastNameError.setText(shown(lastName, problems, SignUpProblem.LAST_NAME, AccountStrings.ERROR_LAST_NAME));
        String usernameText = "";
        if (!username.getText().isEmpty()) {
            if (problems.contains(SignUpProblem.USERNAME_FORMAT)) {
                usernameText = AccountStrings.ERROR_USERNAME_FORMAT;
            } else if (problems.contains(SignUpProblem.USERNAME_TAKEN)) {
                usernameText = AccountStrings.ERROR_USERNAME_TAKEN;
            }
        }
        usernameError.setText(usernameText);
        String passwordText = "";
        if (!password.getText().isEmpty()) {
            if (problems.contains(SignUpProblem.PASSWORD_TOO_SHORT)) {
                passwordText = AccountStrings.ERROR_PASSWORD_SHORT;
            } else if (problems.contains(SignUpProblem.PASSWORD_TOO_LONG)) {
                passwordText = AccountStrings.ERROR_PASSWORD_LONG;
            }
        }
        passwordError.setText(passwordText);
        repeatError.setText(!repeat.getText().isEmpty() && problems.contains(SignUpProblem.PASSWORDS_DIFFER)
                ? AccountStrings.ERROR_PASSWORDS_DIFFER : "");
        birthdayError.setText(year != 0 && problems.contains(SignUpProblem.BIRTHDAY) ? AccountStrings.ERROR_BIRTHDAY : "");

        dayValue.setText(year == 0 ? AccountStrings.DAY : String.valueOf(day));
        monthValue.setText(year == 0 ? AccountStrings.MONTH : AccountStrings.MONTHS[month - 1]);
        yearValue.setText(year == 0 ? AccountStrings.YEAR : String.valueOf(year));

        boolean stepValid = problems.stream().allMatch(p -> p == SignUpProblem.SAFETY_COLOR);
        setEnabled(next, stepValid);
    }

    private static String shown(TextField field, Set<SignUpProblem> problems, SignUpProblem problem, String text) {
        return !field.getText().isEmpty() && problems.contains(problem) ? text : "";
    }
}
