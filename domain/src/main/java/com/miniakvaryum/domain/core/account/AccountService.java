package com.miniakvaryum.domain.core.account;

import com.miniakvaryum.domain.core.time.Clock;
import com.miniakvaryum.domain.core.util.IdGenerator;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sign-up, sign-in, "Remember me" and password reset with the safety color.
 *
 * <p>Accounts live above the game session: the game first asks this service for an account and
 * then starts the session with that account's save folder ({@link #folderOf}). Nothing here
 * touches the parent PIN: a new account simply has no PIN yet, and the parent area asks for one
 * the first time it is opened, as before.
 *
 * <p>Locks are measured with the wall clock, because they must survive closing the app and no
 * game session exists before sign-in. Setting the clock back does not shorten a lock (see
 * {@link Lockout#remainingMillis}).
 */
public final class AccountService {

    private static final Pattern USERNAME_CHARS = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern NOT_ID_CHAR = Pattern.compile("[^a-z0-9]");
    private static final int ID_LENGTH = 8;

    private final AccountStore store;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final ZoneId zone;
    private final IdGenerator ids;
    private final AccountsConfig config;

    private AccountStore.Data data;
    /** Developer tools only: a pretend date, or null for the real one. */
    private LocalDate pretendToday;

    /**
     * @param store  where the accounts are kept
     * @param hasher hashes passwords and safety colors
     * @param clock  wall clock for locks, birthdays and the sign-up time
     * @param zone   time zone of the device, to turn the clock into today's date
     * @param ids    source of new account ids
     * @param config rules
     */
    public AccountService(AccountStore store, PasswordHasher hasher, Clock clock, ZoneId zone, IdGenerator ids,
            AccountsConfig config) {
        this.store = Objects.requireNonNull(store, "store");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.config = Objects.requireNonNull(config, "config");
        this.data = store.load();
    }

    // ---------------------------------------------------------------- reading

    /** All accounts on this device, in sign-up order. */
    public List<Account> all() {
        return data.accounts();
    }

    /** The account with this id. */
    public Optional<Account> find(String accountId) {
        return data.accounts().stream().filter(a -> a.id().equals(accountId)).findFirst();
    }

    /** The account with this username, ignoring upper and lower case. */
    public Optional<Account> findByUsername(String username) {
        String key = normalize(username);
        return data.accounts().stream().filter(a -> normalize(a.username()).equals(key)).findFirst();
    }

    /** Returns true if no account on this device has this username (ignoring upper and lower case). */
    public boolean isUsernameFree(String username) {
        return findByUsername(username).isEmpty();
    }

    /** The save folder of an account. */
    public Path folderOf(Account account) {
        return store.folderOf(account.id());
    }

    /** Today's date on this device, or the pretend date of the developer tools. */
    public LocalDate today() {
        if (pretendToday != null) {
            return pretendToday;
        }
        return realToday();
    }

    /** Today's date on this device; the pretend date of the developer tools does not change it. */
    public LocalDate realToday() {
        return LocalDate.ofInstant(Instant.ofEpochMilli(clock.wallMillis()), zone);
    }

    /**
     * Developer tools only: makes {@link #today()} return the given day, for example to see the
     * birthday on the profile. Locks still use the real clock. Nothing is saved; the real date is
     * back when the app starts again.
     *
     * @param day the pretend date, or null for the real date
     */
    public void setTodayForDevelopment(LocalDate day) {
        pretendToday = day;
    }

    /** True while the developer tools pretend another date. */
    public boolean isTodayPretended() {
        return pretendToday != null;
    }

    /** Age in full years on the given day. */
    public static int ageOn(LocalDate birthday, LocalDate day) {
        return Period.between(birthday, day).getYears();
    }

    /** The rules this service checks. */
    public AccountsConfig config() {
        return config;
    }

    // ---------------------------------------------------------------- sign-up

    /**
     * Checks everything entered so far. An empty result means the draft can be used for
     * {@link #create}. The UI shows the problems of the current step only.
     */
    public Set<SignUpProblem> check(SignUpDraft draft) {
        Set<SignUpProblem> problems = EnumSet.noneOf(SignUpProblem.class);
        if (!validName(draft.firstName())) {
            problems.add(SignUpProblem.FIRST_NAME);
        }
        if (!validName(draft.lastName())) {
            problems.add(SignUpProblem.LAST_NAME);
        }
        String username = draft.username() == null ? "" : draft.username();
        if (username.length() < config.usernameMinLength() || username.length() > config.usernameMaxLength()
                || !USERNAME_CHARS.matcher(username).matches()) {
            problems.add(SignUpProblem.USERNAME_FORMAT);
        } else if (!isUsernameFree(username)) {
            problems.add(SignUpProblem.USERNAME_TAKEN);
        }
        passwordProblem(draft.password(), draft.repeatPassword()).ifPresent(problems::add);
        if (!validBirthday(draft.birthday())) {
            problems.add(SignUpProblem.BIRTHDAY);
        }
        if (draft.safetyColor() == null) {
            problems.add(SignUpProblem.SAFETY_COLOR);
        }
        return problems;
    }

    /**
     * Creates the account and saves it. The first account on a device that already has a save
     * from before accounts existed takes over that save (with its parent PIN).
     *
     * @throws IllegalArgumentException if {@link #check} finds a problem
     */
    public Account create(SignUpDraft draft) {
        Set<SignUpProblem> problems = check(draft);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Sign-up data is not valid: " + problems);
        }
        Account account = new Account(newId(), draft.username(), draft.firstName().trim(), draft.lastName().trim(),
                draft.birthday(), hasher.hash(draft.password()), hasher.hash(draft.safetyColor().name()),
                draft.picture() == null ? Picture.initial() : draft.picture(), Lockout.NONE, Lockout.NONE,
                clock.wallMillis());
        boolean first = data.accounts().isEmpty();
        List<Account> accounts = new ArrayList<>(data.accounts());
        accounts.add(account);
        write(new AccountStore.Data(data.rememberedId(), accounts));
        if (first) {
            store.moveLegacySaveTo(account.id());
        }
        return account;
    }

    // ---------------------------------------------------------------- sign-in

    /**
     * Checks username and password. After too many wrong passwords for one account, that account
     * is locked for a while. An unknown username is reported exactly like a wrong password.
     */
    public SignInResult signIn(String username, String password) {
        Optional<Account> found = findByUsername(username == null ? "" : username);
        if (found.isEmpty()) {
            return SignInResult.wrong();
        }
        Account account = found.get();
        long now = clock.wallMillis();
        long remaining = account.signIn().remainingMillis(now);
        if (remaining > 0) {
            return SignInResult.locked(remaining);
        }
        if (hasher.matches(account.password(), password == null ? "" : password)) {
            Account cleared = account.withSignIn(Lockout.NONE);
            replace(cleared);
            return SignInResult.ok(cleared);
        }
        Lockout next = account.signIn().wrong(now, config.signInMaxFailed(), config.signInWaitSeconds());
        replace(account.withSignIn(next));
        long lock = next.remainingMillis(now);
        return lock > 0 ? SignInResult.locked(lock) : SignInResult.wrong();
    }

    // ---------------------------------------------------------------- forgot password

    /**
     * First step of "Forgot password?": checks the safety color. A wrong color counts towards the
     * lock. Returns {@code OK} if the color is right; nothing else changes then.
     *
     * @throws IllegalArgumentException if there is no account with this id
     */
    public ResetResult checkSafetyColor(String accountId, SafetyColor color) {
        Account account = require(accountId);
        long now = clock.wallMillis();
        long remaining = account.safety().remainingMillis(now);
        if (remaining > 0) {
            return new ResetResult(ResetResult.Status.LOCKED, Optional.empty(), remaining);
        }
        if (color != null && hasher.matches(account.safetyColor(), color.name())) {
            return ResetResult.of(ResetResult.Status.OK);
        }
        Lockout next = account.safety().wrong(now, config.safetyMaxFailed(), config.safetyWaitSeconds());
        replace(account.withSafety(next));
        long lock = next.remainingMillis(now);
        return lock > 0 ? new ResetResult(ResetResult.Status.LOCKED, Optional.empty(), lock)
                : ResetResult.of(ResetResult.Status.WRONG_COLOR);
    }

    /**
     * Second step of "Forgot password?": sets a new password if the safety color is right. The
     * color is checked again (and counted if wrong), so this method is safe on its own.
     *
     * @throws IllegalArgumentException if there is no account with this id
     */
    public ResetResult resetPassword(String accountId, SafetyColor color, String newPassword, String repeatPassword) {
        ResetResult colorCheck = checkSafetyColor(accountId, color);
        if (colorCheck.status() != ResetResult.Status.OK) {
            return colorCheck;
        }
        if (passwordProblem(newPassword, repeatPassword).isPresent()) {
            return ResetResult.of(ResetResult.Status.INVALID_PASSWORD);
        }
        Account updated = require(accountId).withPassword(hasher.hash(newPassword))
                .withSignIn(Lockout.NONE).withSafety(Lockout.NONE);
        replace(updated);
        return new ResetResult(ResetResult.Status.OK, Optional.of(updated), 0L);
    }

    /** Returns the problem of a new password, if any. */
    public Optional<SignUpProblem> passwordProblem(String password, String repeatPassword) {
        String value = password == null ? "" : password;
        if (value.length() < config.passwordMinLength()) {
            return Optional.of(SignUpProblem.PASSWORD_TOO_SHORT);
        }
        if (value.length() > config.passwordMaxLength()) {
            return Optional.of(SignUpProblem.PASSWORD_TOO_LONG);
        }
        if (!value.equals(repeatPassword)) {
            return Optional.of(SignUpProblem.PASSWORDS_DIFFER);
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- profile edits

    /**
     * Changes first and last name.
     *
     * @return {@code OK}, or {@code INVALID} if a name is empty or too long (nothing changed)
     */
    public EditResult changeNames(String accountId, String firstName, String lastName) {
        Account account = require(accountId);
        if (!validName(firstName) || !validName(lastName)) {
            return EditResult.INVALID;
        }
        replace(account.withNames(firstName.trim(), lastName.trim()));
        return EditResult.OK;
    }

    /** Changes the profile picture. For a photo, the game writes {@code photo.png} into {@link #folderOf} first. */
    public void changePicture(String accountId, Picture picture) {
        Objects.requireNonNull(picture, "picture");
        replace(require(accountId).withPicture(picture));
    }

    /**
     * Changes the password; the current password must be given.
     *
     * @return {@code OK}, {@code WRONG_PASSWORD}, or {@code INVALID} if the new password breaks
     *         the rules or the two entries differ
     */
    public EditResult changePassword(String accountId, String currentPassword, String newPassword,
            String repeatPassword) {
        Account account = require(accountId);
        if (!hasher.matches(account.password(), currentPassword == null ? "" : currentPassword)) {
            return EditResult.WRONG_PASSWORD;
        }
        if (passwordProblem(newPassword, repeatPassword).isPresent()) {
            return EditResult.INVALID;
        }
        replace(account.withPassword(hasher.hash(newPassword)));
        return EditResult.OK;
    }

    /**
     * Changes the safety color; the current password must be given.
     *
     * @return {@code OK}, {@code WRONG_PASSWORD}, or {@code INVALID} if no color is given
     */
    public EditResult changeSafetyColor(String accountId, String currentPassword, SafetyColor color) {
        Account account = require(accountId);
        if (!hasher.matches(account.password(), currentPassword == null ? "" : currentPassword)) {
            return EditResult.WRONG_PASSWORD;
        }
        if (color == null) {
            return EditResult.INVALID;
        }
        replace(account.withSafetyColor(hasher.hash(color.name())).withSafety(Lockout.NONE));
        return EditResult.OK;
    }

    /**
     * Returns true on the birthday. Someone born on 29 February celebrates on 28 February in
     * years without a 29 February.
     */
    public static boolean isBirthday(LocalDate birthday, LocalDate day) {
        if (birthday.getMonthValue() == 2 && birthday.getDayOfMonth() == 29 && !day.isLeapYear()) {
            return day.getMonthValue() == 2 && day.getDayOfMonth() == 28;
        }
        return birthday.getMonthValue() == day.getMonthValue() && birthday.getDayOfMonth() == day.getDayOfMonth();
    }

    // ---------------------------------------------------------------- remember me

    /** The account that opens directly at start-up, if "Remember me" was ticked. */
    public Optional<Account> remembered() {
        return data.rememberedId().isEmpty() ? Optional.empty() : find(data.rememberedId());
    }

    /** Remembers this account; replaces any account remembered before. */
    public void remember(String accountId) {
        require(accountId);
        write(new AccountStore.Data(accountId, data.accounts()));
    }

    /** Forgets the remembered account (sign-out). */
    public void forget() {
        if (!data.rememberedId().isEmpty()) {
            write(new AccountStore.Data("", data.accounts()));
        }
    }

    // ---------------------------------------------------------------- helpers

    private boolean validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return !trimmed.isEmpty() && trimmed.length() <= config.nameMaxLength();
    }

    private boolean validBirthday(LocalDate birthday) {
        if (birthday == null) {
            return false;
        }
        LocalDate today = today();
        if (birthday.isAfter(today)) {
            return false;
        }
        int age = ageOn(birthday, today);
        return age >= config.minAge() && age <= config.maxAge();
    }

    private Account require(String accountId) {
        return find(accountId).orElseThrow(() -> new IllegalArgumentException("No account with id " + accountId));
    }

    /** A short id made of lower-case letters and digits that no other account uses. */
    private String newId() {
        while (true) {
            String raw = NOT_ID_CHAR.matcher(ids.next().toLowerCase(Locale.ROOT)).replaceAll("");
            String id = raw.length() > ID_LENGTH ? raw.substring(0, ID_LENGTH) : raw;
            if (!id.isEmpty() && find(id).isEmpty()) {
                return id;
            }
        }
    }

    private void replace(Account updated) {
        List<Account> accounts = new ArrayList<>(data.accounts());
        accounts.replaceAll(a -> a.id().equals(updated.id()) ? updated : a);
        write(new AccountStore.Data(data.rememberedId(), accounts));
    }

    private void write(AccountStore.Data next) {
        store.save(next);
        data = next;
    }

    private static String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
