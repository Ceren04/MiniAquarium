package com.miniakvaryum.game.feature.tasks;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.feature.security.api.ParentAccessException;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.tasks.TasksConfig;
import com.miniakvaryum.domain.feature.tasks.api.ApprovalResult;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.feature.tasks.api.TaskStatus;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.BaseScreen;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;

/**
 * The parent area (specification section 10.5). It is opened by the PIN gate after a correct
 * PIN, so the {@code ParentSession} is unlocked while it is visible.
 *
 * <ul>
 *   <li>Left: a form for a new task (title, coins, food, {@link TasksStrings#CREATE_TASK}).
 *       {@link TasksStrings#EDIT} loads an ACTIVE task into the same form; it then saves with
 *       {@link TasksStrings#SAVE_CHANGES} or is reset with {@link TasksStrings#CANCEL}.</li>
 *   <li>Right: all tasks. ACTIVE: Edit and Delete. PENDING: Approve and Reject.</li>
 *   <li>{@link TasksStrings#LOCK_AND_BACK} locks and returns to the main screen. Leaving the
 *       screen in any other way locks too ({@link #hide()}).</li>
 *   <li>Domain errors (validation, wrong state, locked) are shown as short messages.</li>
 * </ul>
 */
final class ParentScreen extends BaseScreen {

    private static final float FORM_WIDTH = 380f;
    private static final float FIELD_HEIGHT = 56f;
    private static final float LIST_WIDTH = 780f;
    private static final float LIST_HEIGHT = 470f;
    private static final float TASK_TITLE_WIDTH = 300f;
    private static final float MESSAGE_SECONDS = 2.5f;
    /** The game uses the default tasks config (see DomainFeatures); the domain checks again. */
    private static final TasksConfig LIMITS = TasksConfig.defaults();

    private final UiContext ui;
    private final TaskService tasks;
    private final ParentSession parent;

    private final Label formTitle;
    private final TextField titleField;
    private final TextField coinsField;
    private final TextField foodField;
    private final TextButton submitButton;
    private final TextButton cancelButton;
    private final Table list = new Table();

    /** Id of the task loaded into the form by "Edit"; null while the form creates a new task. */
    private String editingId;

    ParentScreen(UiContext ui) {
        super(ui.skin());
        this.ui = ui;
        this.tasks = ui.session().get(TaskService.class);
        this.parent = ui.session().get(ParentSession.class);

        Table root = newRootTable();
        root.add(new Label(TasksStrings.PARENT_TITLE, skin, UiSkin.TITLE)).colspan(2).padBottom(UiConfig.GAP * 2).row();

        // ---- form
        Table form = new Table();
        form.top().left();
        formTitle = new Label(TasksStrings.NEW_TASK, skin);
        form.add(formTitle).left().padBottom(UiConfig.GAP).row();
        titleField = field(TasksStrings.TITLE_FIELD, false);
        coinsField = field(TasksStrings.COINS_FIELD, true);
        foodField = field(TasksStrings.FOOD_FIELD, true);
        addField(form, TasksStrings.TITLE_FIELD, titleField);
        addField(form, TasksStrings.COINS_FIELD, coinsField);
        addField(form, TasksStrings.FOOD_FIELD, foodField);
        submitButton = button(TasksStrings.CREATE_TASK, this::submitForm);
        form.add(submitButton).width(FORM_WIDTH).height(UiConfig.MIN_TOUCH_SIZE).padTop(UiConfig.GAP).row();
        cancelButton = button(TasksStrings.CANCEL, this::resetForm);
        form.add(cancelButton).width(FORM_WIDTH).height(UiConfig.MIN_TOUCH_SIZE).padTop(UiConfig.GAP).row();

        // ---- list
        Table right = new Table();
        right.top().left();
        right.add(new Label(TasksStrings.ALL_TASKS, skin)).left().padBottom(UiConfig.GAP).row();
        list.top().left();
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFadeScrollBars(false);
        right.add(scroll).size(LIST_WIDTH, LIST_HEIGHT);

        root.add(form).top().width(FORM_WIDTH).padRight(UiConfig.GAP * 3);
        root.add(right).top().row();
        root.add(button(TasksStrings.LOCK_AND_BACK, this::lockAndGoBack))
                .colspan(2).minSize(UiConfig.MIN_TOUCH_SIZE * 4, UiConfig.MIN_TOUCH_SIZE).padTop(UiConfig.GAP * 2);
    }

    private TextField field(String hint, boolean digitsOnly) {
        TextField field = new TextField("", skin);
        field.setMessageText(hint);
        if (digitsOnly) {
            field.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
            field.setMaxLength(String.valueOf(LIMITS.rewardMax()).length());
        }
        return field;
    }

    private void addField(Table form, String label, TextField field) {
        form.add(new Label(label, skin)).left().row();
        form.add(field).width(FORM_WIDTH).height(FIELD_HEIGHT).padBottom(UiConfig.GAP).row();
    }

    @Override
    protected void onShow() {
        resetForm();
        rebuild();
    }

    /** Leaving the parent area in any way locks it (also when the window is closed). */
    @Override
    public void hide() {
        parent.lock();
        resetForm();
    }

    // ------------------------------------------------------------ actions

    private void submitForm() {
        int coins = number(coinsField);
        int food = number(foodField);
        String title = titleField.getText();
        boolean done = act(() -> {
            if (editingId == null) {
                tasks.create(title, coins, food);
                showMessage(TasksStrings.TASK_CREATED, MESSAGE_SECONDS);
            } else {
                tasks.update(editingId, title, coins, food);
                showMessage(TasksStrings.TASK_SAVED, MESSAGE_SECONDS);
            }
        });
        if (done) {
            resetForm();
        }
        rebuild();
    }

    private void edit(Task task) {
        editingId = task.id();
        formTitle.setText(TasksStrings.EDIT_TASK);
        titleField.setText(task.title());
        coinsField.setText(String.valueOf(task.coinReward()));
        foodField.setText(String.valueOf(task.foodReward()));
        submitButton.setText(TasksStrings.SAVE_CHANGES);
        cancelButton.setVisible(true);
    }

    private void delete(Task task) {
        act(() -> tasks.delete(task.id()));
        if (task.id().equals(editingId)) {
            resetForm();
        }
        rebuild();
    }

    private void approve(Task task) {
        act(() -> {
            ApprovalResult result = tasks.approve(task.id());
            showMessage(result == ApprovalResult.APPROVED
                    ? String.format(TasksStrings.APPROVED_MESSAGE, task.coinReward(), task.foodReward())
                    : TasksStrings.ALREADY_APPROVED, MESSAGE_SECONDS);
        });
        rebuild();
    }

    private void reject(Task task) {
        act(() -> tasks.reject(task.id()));
        rebuild();
    }

    private void lockAndGoBack() {
        parent.lock();
        ui.nav().showMain();
    }

    /**
     * Runs a domain call and turns its expected failures into a short message.
     *
     * @return true if the call succeeded
     */
    private boolean act(Runnable call) {
        try {
            call.run();
            return true;
        } catch (ParentAccessException e) {
            showMessage(TasksStrings.LOCKED, MESSAGE_SECONDS);
        } catch (IllegalArgumentException e) {
            showMessage(String.format(TasksStrings.INVALID_TASK, LIMITS.titleMaxLength(), LIMITS.rewardMax()),
                    MESSAGE_SECONDS);
        } catch (IllegalStateException e) {
            showMessage(TasksStrings.NOT_POSSIBLE, MESSAGE_SECONDS);
        }
        return false;
    }

    // ------------------------------------------------------------ display

    private void resetForm() {
        editingId = null;
        formTitle.setText(TasksStrings.NEW_TASK);
        titleField.setText("");
        coinsField.setText("");
        foodField.setText("");
        submitButton.setText(TasksStrings.CREATE_TASK);
        cancelButton.setVisible(false);
        stage.setKeyboardFocus(null);
    }

    private void rebuild() {
        list.clearChildren();
        if (tasks.list().isEmpty()) {
            list.add(new Label(TasksStrings.NO_TASKS, skin)).pad(UiConfig.GAP);
            return;
        }
        // Reported tasks first: they are what the parent has to act on.
        for (TaskStatus status : new TaskStatus[] {TaskStatus.PENDING, TaskStatus.ACTIVE, TaskStatus.APPROVED}) {
            for (Task task : tasks.list(status)) {
                addRow(task);
            }
        }
    }

    private void addRow(Task task) {
        Label title = new Label(task.title(), skin);
        title.setEllipsis(true);
        title.setAlignment(Align.left);
        list.add(title).width(TASK_TITLE_WIDTH).height(UiConfig.MIN_TOUCH_SIZE).pad(UiConfig.GAP / 2);
        list.add(new Label(String.format(TasksStrings.REWARD, task.coinReward(), task.foodReward()), skin))
                .pad(UiConfig.GAP / 2);
        switch (task.status()) {
            case ACTIVE -> {
                addRowButton(TasksStrings.EDIT, () -> edit(task));
                addRowButton(TasksStrings.DELETE, () -> delete(task));
            }
            case PENDING -> {
                addRowButton(TasksStrings.APPROVE, () -> approve(task));
                addRowButton(TasksStrings.REJECT, () -> reject(task));
            }
            case APPROVED -> list.add(new Label(TasksStrings.STATUS_APPROVED, skin)).colspan(2).pad(UiConfig.GAP / 2);
        }
        list.row();
    }

    private void addRowButton(String text, Runnable action) {
        list.add(button(text, action)).minSize(UiConfig.MIN_TOUCH_SIZE * 2, UiConfig.MIN_TOUCH_SIZE).pad(UiConfig.GAP / 4);
    }

    /** Empty means 0. The digits-only filter and the length limit keep the text a small number. */
    private static int number(TextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }
}
