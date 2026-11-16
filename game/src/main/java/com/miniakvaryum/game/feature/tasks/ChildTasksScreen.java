package com.miniakvaryum.game.feature.tasks;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.feature.tasks.api.TaskStatus;
import com.miniakvaryum.game.core.CoreStrings;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.BaseScreen;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.List;

/**
 * The task list the child sees (specification section 10.5). Needs no parent PIN.
 *
 * <ul>
 *   <li>ACTIVE tasks: title, rewards and a button {@link TasksStrings#DONE} that calls
 *       {@code reportDone}.</li>
 *   <li>PENDING tasks: title and {@link TasksStrings#WAITING_FOR_PARENTS}.</li>
 *   <li>The number of APPROVED tasks, and a back button.</li>
 * </ul>
 * The list is rebuilt in {@link #onShow()} and after every action.
 */
final class ChildTasksScreen extends BaseScreen {

    private static final float TITLE_WIDTH = 520f;
    private static final float LIST_WIDTH = 1100f;
    private static final float LIST_HEIGHT = 440f;

    private final UiContext ui;
    private final TaskService tasks;
    private final Table list = new Table();
    private final Label approvedCount;

    ChildTasksScreen(UiContext ui) {
        super(ui.skin());
        this.ui = ui;
        this.tasks = ui.session().get(TaskService.class);

        Table root = newRootTable();
        root.add(new Label(TasksStrings.CHILD_TITLE, skin, UiSkin.TITLE)).padBottom(UiConfig.GAP * 2).row();

        list.top();
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFadeScrollBars(false);
        root.add(scroll).size(LIST_WIDTH, LIST_HEIGHT).padBottom(UiConfig.GAP * 2).row();

        approvedCount = new Label("", skin);
        root.add(approvedCount).padBottom(UiConfig.GAP * 2).row();
        root.add(button(CoreStrings.BACK, ui.nav()::showMain)).minSize(UiConfig.MIN_TOUCH_SIZE * 3, UiConfig.MIN_TOUCH_SIZE);
    }

    @Override
    protected void onShow() {
        rebuild();
    }

    private void rebuild() {
        list.clearChildren();
        List<Task> open = tasks.list(TaskStatus.ACTIVE);
        List<Task> waiting = tasks.list(TaskStatus.PENDING);
        if (open.isEmpty() && waiting.isEmpty()) {
            list.add(new Label(TasksStrings.NO_TASKS, skin)).pad(UiConfig.GAP);
        }
        for (Task task : open) {
            addTitleAndReward(task);
            list.add(button(TasksStrings.DONE, () -> reportDone(task)))
                    .minSize(UiConfig.MIN_TOUCH_SIZE * 2, UiConfig.MIN_TOUCH_SIZE).pad(UiConfig.GAP / 2);
            list.row();
        }
        for (Task task : waiting) {
            addTitleAndReward(task);
            list.add(new Label(TasksStrings.WAITING_FOR_PARENTS, skin)).pad(UiConfig.GAP / 2);
            list.row();
        }
        approvedCount.setText(String.format(TasksStrings.APPROVED_COUNT, tasks.list(TaskStatus.APPROVED).size()));
    }

    private void addTitleAndReward(Task task) {
        Label title = new Label(task.title(), skin);
        title.setEllipsis(true);
        title.setAlignment(Align.left);
        list.add(title).width(TITLE_WIDTH).height(UiConfig.MIN_TOUCH_SIZE).pad(UiConfig.GAP / 2);
        list.add(new Label(String.format(TasksStrings.REWARD, task.coinReward(), task.foodReward()), skin))
                .pad(UiConfig.GAP / 2);
    }

    private void reportDone(Task task) {
        try {
            tasks.reportDone(task.id());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // The list was out of date (for example the parent changed the task meanwhile).
            showMessage(TasksStrings.NOT_POSSIBLE, 2f);
        }
        rebuild();
    }
}
