package com.miniakvaryum.game.feature.tasks;

import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.PlaceholderScreen;

/**
 * The task list the child sees.
 *
 * <p><b>SKELETON</b> (specification section 8.5). To implement, with
 * {@code TaskService tasks = ui.session().get(TaskService.class)}:
 * <ul>
 *   <li>Rebuild the list in {@code onShow()} and after every action.</li>
 *   <li>ACTIVE tasks: title, rewards and a button {@link TasksStrings#DONE} that calls
 *       {@code tasks.reportDone(id)}.</li>
 *   <li>PENDING tasks: title and the text {@link TasksStrings#WAITING_FOR_PARENTS}.</li>
 *   <li>A line {@link TasksStrings#APPROVED_COUNT} with the number of APPROVED tasks.</li>
 *   <li>A back button to the main screen.</li>
 *   <li>Use a ScrollPane if the list can get longer than the screen.</li>
 * </ul>
 * This screen needs no parent PIN.
 */
final class ChildTasksScreen extends PlaceholderScreen {

    private final UiContext ui;

    ChildTasksScreen(UiContext ui) {
        super(ui.skin(), ui.nav(), TasksStrings.CHILD_TITLE);
        this.ui = ui;
    }
}
