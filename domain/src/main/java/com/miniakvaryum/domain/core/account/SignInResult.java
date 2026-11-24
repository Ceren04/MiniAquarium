package com.miniakvaryum.domain.core.account;

import java.util.Optional;

/**
 * Outcome of {@link AccountService#signIn}.
 *
 * @param status                what happened
 * @param account               the signed-in account, only for {@link Status#OK}
 * @param lockedRemainingMillis time until the lock ends, only for {@link Status#LOCKED}
 */
public record SignInResult(Status status, Optional<Account> account, long lockedRemainingMillis) {

    /** What happened. */
    public enum Status {
        OK,
        /** Unknown username or wrong password; the UI does not say which. */
        WRONG,
        /** Too many wrong passwords for this account; wait {@code lockedRemainingMillis}. */
        LOCKED
    }

    static SignInResult ok(Account account) {
        return new SignInResult(Status.OK, Optional.of(account), 0L);
    }

    static SignInResult wrong() {
        return new SignInResult(Status.WRONG, Optional.empty(), 0L);
    }

    static SignInResult locked(long remainingMillis) {
        return new SignInResult(Status.LOCKED, Optional.empty(), remainingMillis);
    }
}
