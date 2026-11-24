package com.miniakvaryum.domain.core.account;

import java.time.LocalDate;

/**
 * What the child entered during sign-up. Lives only in memory; nothing is saved until the last
 * step creates the account. Any field may still be missing (null) while the child is typing.
 *
 * @param firstName      first name
 * @param lastName       last name
 * @param username       username
 * @param password       password
 * @param repeatPassword the password typed a second time
 * @param birthday       date of birth
 * @param safetyColor    the chosen safety color
 * @param picture        the chosen picture
 */
public record SignUpDraft(String firstName, String lastName, String username, String password,
        String repeatPassword, LocalDate birthday, SafetyColor safetyColor, Picture picture) {

    /** Nothing entered yet. */
    public static SignUpDraft empty() {
        return new SignUpDraft("", "", "", "", "", null, null, Picture.initial());
    }

    public SignUpDraft withAboutYou(String newFirstName, String newLastName, String newUsername, String newPassword,
            String newRepeatPassword, LocalDate newBirthday) {
        return new SignUpDraft(newFirstName, newLastName, newUsername, newPassword, newRepeatPassword, newBirthday,
                safetyColor, picture);
    }

    public SignUpDraft withSafetyColor(SafetyColor newSafetyColor) {
        return new SignUpDraft(firstName, lastName, username, password, repeatPassword, birthday, newSafetyColor,
                picture);
    }

    public SignUpDraft withPicture(Picture newPicture) {
        return new SignUpDraft(firstName, lastName, username, password, repeatPassword, birthday, safetyColor,
                newPicture);
    }
}
