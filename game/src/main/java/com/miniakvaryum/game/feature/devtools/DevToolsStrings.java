package com.miniakvaryum.game.feature.devtools;

/** User-visible texts of the developer tools. */
final class DevToolsStrings {

    static final String ADD_FOOD = "+5 food";
    static final String NOT_ADDED = "Food was not added";
    static final String JUMP_1H = "+1 h";
    static final String JUMP_6H = "+6 h";
    static final String JUMP_24H = "+24 h";
    static final String UNDO_TIME = "Undo time";
    static final String ENERGY_0 = "E: 0";
    static final String ENERGY_100 = "E: 100";
    static final String NOTHING_TO_UNDO = "No time jump to undo";
    static final String TIME_FAILED = "Time travel failed, see the log";
    /** Use with String.format: hours, then minutes, ahead of the checkpoint. */
    static final String AHEAD = "Time: +%d h %02d min";

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

    private DevToolsStrings() {
    }
}
