package com.miniakvaryum.domain.core.account;

/** Reasons why sign-up data is not accepted. The UI turns each one into a message. */
public enum SignUpProblem {
    FIRST_NAME,
    LAST_NAME,
    /** Too short, too long, or characters other than letters, digits and underscore. */
    USERNAME_FORMAT,
    /** Another account on this device has the same username (ignoring upper and lower case). */
    USERNAME_TAKEN,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    PASSWORDS_DIFFER,
    /** Missing, in the future, or the age is outside the allowed range. */
    BIRTHDAY,
    SAFETY_COLOR
}
