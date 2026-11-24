package com.miniakvaryum.game.feature.devtools;

/** User-visible texts of the developer tools. */
final class DevToolsStrings {

    static final String ADD_FOOD = "+5 food";
    static final String NOT_ADDED = "Food was not added";
    static final String JUMP_1H = "+1 h";
    static final String JUMP_6H = "+6 h";
    static final String JUMP_24H = "+24 h";
    static final String UNDO_TIME = "Undo time";
    static final String CLOCK_REAL = "Clock: real";
    static final String CLOCK_AT = "Clock: %s";
    static final String CLOCK_SHIFTED = "Clock: shifted";
    static final String NOTHING_TO_UNDO = "No time jump to undo";
    static final String TIME_FAILED = "Time travel failed, see the log";
    /** Use with String.format: hours, then minutes, ahead of the checkpoint. */
    static final String AHEAD = "+%d h %02d min";

    private DevToolsStrings() {
    }
}
