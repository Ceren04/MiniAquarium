package com.miniakvaryum.game.feature.tasks;

/** User-visible texts of the task screens. */
final class TasksStrings {

    static final String TASKS_BUTTON = "Tasks";
    static final String CHILD_TITLE = "Tasks";
    static final String PARENT_TITLE = "Parent area";
    static final String DONE = "Done";
    static final String WAITING_FOR_PARENTS = "Waiting for parents";
    /** Use with String.format: number of approved tasks. */
    static final String APPROVED_COUNT = "Completed: %d";
    static final String CREATE_TASK = "Create task";
    static final String EDIT = "Edit";
    static final String DELETE = "Delete";
    static final String APPROVE = "Approve";
    static final String REJECT = "Reject";
    static final String LOCK_AND_BACK = "Lock and go back";
    static final String TITLE_FIELD = "Title";
    static final String COINS_FIELD = "Coins";
    static final String FOOD_FIELD = "Food";
    static final String SAVE_CHANGES = "Save changes";
    static final String CANCEL = "Cancel";
    static final String NEW_TASK = "New task";
    static final String EDIT_TASK = "Edit task";
    static final String ALL_TASKS = "All tasks";
    static final String NO_TASKS = "No tasks yet";
    /** Use with String.format: coins, food. */
    static final String REWARD = "%d coins, %d food";
    static final String STATUS_ACTIVE = "Open";
    static final String STATUS_PENDING = "Done? Check it";
    static final String STATUS_APPROVED = "Approved";
    static final String TASK_CREATED = "Task created";
    static final String TASK_SAVED = "Task saved";
    /** Use with String.format: coins, food. */
    static final String APPROVED_MESSAGE = "Approved: +%d coins, +%d food";
    static final String ALREADY_APPROVED = "This task was already approved";
    /** Use with String.format: longest title, highest reward. */
    static final String INVALID_TASK = "Title 1 to %d characters, rewards 0 to %d";
    static final String NOT_POSSIBLE = "This is not possible for this task any more";
    static final String LOCKED = "The parent area is locked";

    private TasksStrings() {
    }
}
