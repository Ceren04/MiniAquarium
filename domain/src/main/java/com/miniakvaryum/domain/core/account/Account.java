package com.miniakvaryum.domain.core.account;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One account on this device. Every account has its own save folder, so its aquarium, fish,
 * coins, tasks and parent PIN are completely separate from the other accounts.
 *
 * @param id             fixed random id; also the name of the save folder ({@code accounts/<id>})
 * @param username       as typed at sign-up; unique on the device, ignoring upper and lower case
 * @param firstName      first name
 * @param lastName       last name
 * @param birthday       date of birth
 * @param password       salted hash of the password
 * @param safetyColor    salted hash of the safety color
 * @param picture        profile picture
 * @param signIn         wrong passwords and lock
 * @param safety         wrong safety colors and lock
 * @param createdAtMillis wall-clock time of the sign-up
 */
public record Account(String id, String username, String firstName, String lastName, LocalDate birthday,
        Secret password, Secret safetyColor, Picture picture, Lockout signIn, Lockout safety,
        long createdAtMillis) {

    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(birthday, "birthday");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(safetyColor, "safetyColor");
        Objects.requireNonNull(picture, "picture");
        Objects.requireNonNull(signIn, "signIn");
        Objects.requireNonNull(safety, "safety");
    }

    Account withPassword(Secret newPassword) {
        return new Account(id, username, firstName, lastName, birthday, newPassword, safetyColor, picture,
                signIn, safety, createdAtMillis);
    }

    Account withSignIn(Lockout newSignIn) {
        return new Account(id, username, firstName, lastName, birthday, password, safetyColor, picture,
                newSignIn, safety, createdAtMillis);
    }

    Account withSafety(Lockout newSafety) {
        return new Account(id, username, firstName, lastName, birthday, password, safetyColor, picture,
                signIn, newSafety, createdAtMillis);
    }
}
