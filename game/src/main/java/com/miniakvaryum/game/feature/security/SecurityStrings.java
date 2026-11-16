package com.miniakvaryum.game.feature.security;

/** User-visible texts of the parent gate. */
final class SecurityStrings {

    static final String PARENTS_BUTTON = "Parents";
    static final String GATE_TITLE = "Parent area";
    static final String SET_PIN = "Set a PIN";
    static final String REPEAT_PIN = "Enter the same PIN again";
    static final String ENTER_PIN = "Enter PIN";
    static final String PINS_DIFFERENT = "The two PINs are different. Try again.";
    /** Use with String.format: number of digits of a PIN. */
    static final String WRONG_LENGTH = "Please enter %d digits";
    static final String CLEAR = "Clear";
    static final String OK = "OK";
    /** Use with String.format: number of attempts left. */
    static final String WRONG_PIN = "Wrong PIN, %d attempts left";
    /** Use with String.format: seconds until the lock ends. */
    static final String LOCKED = "Locked, %d seconds left";

    private SecurityStrings() {
    }
}
