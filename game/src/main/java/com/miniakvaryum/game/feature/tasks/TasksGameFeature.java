package com.miniakvaryum.game.feature.tasks;

import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * UI of the tasks feature: the "Tasks" button, the child's task list and the parent area.
 */
public final class TasksGameFeature implements GameFeature {

    /** Id of the child's task list. */
    public static final String SCREEN_CHILD_TASKS = "tasks";

    /** Id of the parent area. It is opened by the PIN gate, never directly from the main screen. */
    public static final String SCREEN_PARENT_AREA = "parent-area";

    @Override
    public String id() {
        return "tasks";
    }

    @Override
    public void install(UiContext ui) {
        ui.nav().register(SCREEN_CHILD_TASKS, () -> new ChildTasksScreen(ui));
        ui.nav().register(SCREEN_PARENT_AREA, () -> new ParentScreen(ui));
        ui.main().addButton(TasksStrings.TASKS_BUTTON, () -> ui.nav().show(SCREEN_CHILD_TASKS));
    }
}
