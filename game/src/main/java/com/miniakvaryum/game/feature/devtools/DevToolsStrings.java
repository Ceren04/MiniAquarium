package com.miniakvaryum.game.feature.devtools;

/** User-visible texts of the developer tools. */
final class DevToolsStrings {

    static final String ADD_FOOD = "+5 food";
    static final String NOT_ADDED = "Food was not added";
    static final String JUMP_1H = "+1 h";
    static final String JUMP_6H = "+6 h";
    static final String JUMP_24H = "+24 h";
    static final String UNDO_TIME = "Undo";
    static final String ENERGY_0 = "E: 0";
    static final String ENERGY_100 = "E: 100";
    static final String CLOCK_REAL = "Clock: real";
    static final String CLOCK_AT = "Clock: %s";
    static final String CLOCK_SHIFTED = "Clock: shifted";
    static final String NOTHING_TO_UNDO = "No time jump to undo";
    static final String TIME_FAILED = "Time travel failed, see the log";
    /** Use with String.format: hours, then minutes, ahead of the checkpoint. */
    static final String AHEAD = "+%d h %02d min";

    static final String DATE = "Date";
    static final String DATE_TITLE = "Pretend date (birthday test)";
    static final String DATE_DAY = "Day";
    static final String DATE_MONTH = "Month";
    static final String DATE_YEAR = "Year";
    static final String DATE_BIRTHDAY = "Birthday";
    static final String DATE_REAL = "Real date";
    static final String DATE_CLOSE = "Close";
    static final String PLUS = "+";
    static final String MINUS = "-";

    static final String APPROVE_TASKS = "+5 tasks";
    static final String PIN_TITLE = "Parent PIN: approve 5 tasks";
    static final String PIN_EMPTY = "_ _ _ _";
    static final String PIN_DELETE = "Del";
    static final String PIN_OK = "OK";
    static final String PIN_NOT_SET = "No parent PIN yet. Set it in Parents first.";
    static final String PIN_LENGTH = "The PIN has a different length.";
    static final String PIN_WRONG = "Wrong PIN. %d tries left.";
    static final String PIN_LOCKED = "Locked. Try again in %d min.";
    static final String TASKS_APPROVED = "%d tasks approved.";
    static final String DEV_TASK = "Dev task %d";

    private DevToolsStrings() {
    }
}
