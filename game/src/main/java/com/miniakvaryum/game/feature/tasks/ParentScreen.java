package com.miniakvaryum.game.feature.tasks;

import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.PlaceholderScreen;

/**
 * The parent area: create tasks and approve or reject reported ones. It is opened by the PIN
 * gate after a correct PIN, so the {@code ParentSession} is unlocked while it is visible.
 *
 * <p><b>SKELETON</b> (specification section 8.5). To implement, with
 * {@code TaskService tasks = ui.session().get(TaskService.class)}:
 * <ul>
 *   <li>A form for a new task: text fields for title, coins and food and the button
 *       {@link TasksStrings#CREATE_TASK} that calls {@code tasks.create(...)}.</li>
 *   <li>A list of all tasks. ACTIVE tasks get {@link TasksStrings#EDIT} and
 *       {@link TasksStrings#DELETE}. PENDING tasks get {@link TasksStrings#APPROVE} and
 *       {@link TasksStrings#REJECT}.</li>
 *   <li>The button {@link TasksStrings#LOCK_AND_BACK}: call
 *       {@code ui.session().get(ParentSession.class).lock()}, then {@code ui.nav().showMain()}.
 *       Leaving this screen in any other way must lock the session too.</li>
 *   <li>Validation errors ({@code IllegalArgumentException} from the service) are shown with
 *       {@code showMessage(...)} as a short, friendly text, never as a stack trace.</li>
 * </ul>
 */
final class ParentScreen extends PlaceholderScreen {

    private final UiContext ui;

    ParentScreen(UiContext ui) {
        super(ui.skin(), ui.nav(), TasksStrings.PARENT_TITLE);
        this.ui = ui;
    }
}
