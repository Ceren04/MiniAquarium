package com.miniakvaryum.domain.core.account;

import java.util.Optional;

/**
 * Outcome of {@link AccountService#resetPassword}.
 *
 * @param status                what happened
 * @param account               the account with its new password, only for {@link Status#OK}
 * @param lockedRemainingMillis time until the lock ends, only for {@link Status#LOCKED}
 */
public record ResetResult(Status status, Optional<Account> account, long lockedRemainingMillis) {

    /** What happened. */
    public enum Status {
        /** The color was right; the new password is saved and all locks are cleared. */
        OK,
        /** The color was wrong; nothing changed except the counter of wrong colors. */
        WRONG_COLOR,
        /** Too many wrong colors; wait {@code lockedRemainingMillis}. */
        LOCKED,
        /** The new password breaks the password rules or the two entries differ; nothing changed. */
        INVALID_PASSWORD
    }

    static ResetResult of(Status status) {
        return new ResetResult(status, Optional.empty(), 0L);
    }
}
