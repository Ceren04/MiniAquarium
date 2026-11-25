package com.miniakvaryum.domain.core.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.testsupport.FakeClock;
import com.miniakvaryum.domain.testsupport.SequentialIds;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountServiceTest {

    /** 2026-11-24 12:00 UTC. */
    private static final long NOW = 1_795_521_600_000L;
    /** Cheap hashes keep the tests fast; the rules do not depend on the count (as D-028). */
    private static final AccountsConfig CONFIG = new AccountsConfig(30, 3, 15, 6, 64, 3, 17,
            5, List.of(30, 60, 300, 900), 3, List.of(60, 300, 900, 3_600), 1_000);

    @TempDir
    Path home;

    private FakeClock clock;
    private AccountService service;

    @BeforeEach
    void setUp() {
        clock = new FakeClock(NOW, 0L);
        service = newService();
    }

    private AccountService newService() {
        return new AccountService(new AccountStore(home), new PasswordHasher(CONFIG.pbkdf2Iterations()), clock,
                ZoneOffset.UTC, new SequentialIds(), CONFIG);
    }

    private static SignUpDraft draft(String username) {
        return SignUpDraft.empty()
                .withAboutYou("Elif", "Yilmaz", username, "fishes1", "fishes1", LocalDate.of(2017, 5, 3))
                .withSafetyColor(SafetyColor.BLUE)
                .withPicture(Picture.avatar("fish/clown"));
    }

    // ---------------------------------------------------------------- 1. sign-up rules

    @Test
    void aCompleteDraftHasNoProblems() {
        assertThat(service.check(draft("elif_7"))).isEmpty();
    }

    @Test
    void namesMustBePresentAndShort() {
        SignUpDraft d = draft("elif").withAboutYou("  ", "x".repeat(31), "elif", "fishes1", "fishes1",
                LocalDate.of(2017, 5, 3));
        assertThat(service.check(d)).containsExactlyInAnyOrder(SignUpProblem.FIRST_NAME, SignUpProblem.LAST_NAME);
    }

    @Test
    void usernameFormatIsChecked() {
        for (String bad : List.of("el", "a".repeat(16), "elif ok", "elif!", "", "çiçek")) {
            assertThat(service.check(draft(bad))).as(bad).contains(SignUpProblem.USERNAME_FORMAT);
        }
        assertThat(service.check(draft("Elif_2017"))).isEmpty();
    }

    @Test
    void usernameMustBeUniqueIgnoringCase() {
        service.create(draft("Elif"));
        assertThat(service.isUsernameFree("eLIF")).isFalse();
        assertThat(service.check(draft("ELIF"))).containsExactly(SignUpProblem.USERNAME_TAKEN);
        assertThat(service.isUsernameFree("can")).isTrue();
    }

    @Test
    void passwordRules() {
        assertThat(service.passwordProblem("12345", "12345")).contains(SignUpProblem.PASSWORD_TOO_SHORT);
        assertThat(service.passwordProblem("x".repeat(65), "x".repeat(65))).contains(SignUpProblem.PASSWORD_TOO_LONG);
        assertThat(service.passwordProblem("123456", "123457")).contains(SignUpProblem.PASSWORDS_DIFFER);
        assertThat(service.passwordProblem(null, null)).contains(SignUpProblem.PASSWORD_TOO_SHORT);
        assertThat(service.passwordProblem("123456", "123456")).isEmpty();
    }

    @Test
    void birthdayMustGiveAnAgeFromThreeToSeventeen() {
        LocalDate today = service.today();
        assertThat(today).isEqualTo(LocalDate.of(2026, 11, 24));
        assertThat(problemsWithBirthday(today.minusYears(3))).isEmpty();
        assertThat(problemsWithBirthday(today.minusYears(3).plusDays(1))).contains(SignUpProblem.BIRTHDAY);
        assertThat(problemsWithBirthday(today.minusYears(18).plusDays(1))).isEmpty();
        assertThat(problemsWithBirthday(today.minusYears(18))).contains(SignUpProblem.BIRTHDAY);
        assertThat(problemsWithBirthday(today.plusDays(1))).contains(SignUpProblem.BIRTHDAY);
        assertThat(problemsWithBirthday(null)).contains(SignUpProblem.BIRTHDAY);
    }

    private java.util.Set<SignUpProblem> problemsWithBirthday(LocalDate birthday) {
        return service.check(draft("elif").withAboutYou("Elif", "Yilmaz", "elif", "fishes1", "fishes1", birthday));
    }

    @Test
    void safetyColorIsRequired() {
        assertThat(service.check(draft("elif").withSafetyColor(null))).containsExactly(SignUpProblem.SAFETY_COLOR);
    }

    @Test
    void createRefusesAnInvalidDraft() {
        assertThatThrownBy(() -> service.create(draft("e"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.all()).isEmpty();
    }

    @Test
    void createTrimsNamesAndStoresEverything() {
        Account account = service.create(draft("elif").withAboutYou(" Elif ", " Yilmaz", "elif", "fishes1",
                "fishes1", LocalDate.of(2017, 5, 3)));
        assertThat(account.firstName()).isEqualTo("Elif");
        assertThat(account.lastName()).isEqualTo("Yilmaz");
        assertThat(account.picture()).isEqualTo(Picture.avatar("fish/clown"));
        assertThat(account.createdAtMillis()).isEqualTo(NOW);
        assertThat(service.folderOf(account)).isEqualTo(home.resolve("accounts").resolve(account.id()));
        assertThat(account.id()).matches("[a-z0-9]+");
    }

    // ---------------------------------------------------------------- 2. hashing

    @Test
    void passwordAndColorAreHashedNotStoredAsText() throws IOException {
        service.create(draft("elif"));
        String file = Files.readString(home.resolve("accounts.json"), StandardCharsets.UTF_8);
        assertThat(file).contains("\"username\": \"elif\"").doesNotContain("fishes1").doesNotContain("BLUE");
    }

    @Test
    void rightPasswordSignsInWrongDoesNot() {
        service.create(draft("elif"));
        assertThat(service.signIn("ELIF", "fishes1").status()).isEqualTo(SignInResult.Status.OK);
        assertThat(service.signIn("elif", "fishes2").status()).isEqualTo(SignInResult.Status.WRONG);
        assertThat(service.signIn("nobody", "fishes1").status()).isEqualTo(SignInResult.Status.WRONG);
        assertThat(service.signIn(null, null).status()).isEqualTo(SignInResult.Status.WRONG);
    }

    // ---------------------------------------------------------------- 3. lock

    @Test
    void fiveWrongPasswordsLockTheAccountWithGrowingWaits() {
        service.create(draft("elif"));
        for (int i = 0; i < 4; i++) {
            assertThat(service.signIn("elif", "nope").status()).isEqualTo(SignInResult.Status.WRONG);
        }
        SignInResult fifth = service.signIn("elif", "nope");
        assertThat(fifth.status()).isEqualTo(SignInResult.Status.LOCKED);
        assertThat(fifth.lockedRemainingMillis()).isEqualTo(30_000L);
        // Even the right password is refused while locked.
        assertThat(service.signIn("elif", "fishes1").status()).isEqualTo(SignInResult.Status.LOCKED);

        clock.advanceMillis(30_000L);
        for (int i = 0; i < 5; i++) {
            service.signIn("elif", "nope");
        }
        assertThat(service.signIn("elif", "fishes1").lockedRemainingMillis()).isEqualTo(60_000L);

        clock.advanceMillis(60_000L);
        assertThat(service.signIn("elif", "fishes1").status()).isEqualTo(SignInResult.Status.OK);
        // Success clears the counters: the next lock starts again at 30 s.
        for (int i = 0; i < 5; i++) {
            service.signIn("elif", "nope");
        }
        assertThat(service.signIn("elif", "fishes1").lockedRemainingMillis()).isEqualTo(30_000L);
    }

    @Test
    void settingTheClockBackDoesNotShortenALock() {
        service.create(draft("elif"));
        for (int i = 0; i < 5; i++) {
            service.signIn("elif", "nope");
        }
        clock.setWallMillis(NOW - 3_600_000L);
        assertThat(service.signIn("elif", "fishes1").lockedRemainingMillis()).isEqualTo(30_000L);
    }

    @Test
    void lockSurvivesARestart() {
        service.create(draft("elif"));
        for (int i = 0; i < 5; i++) {
            service.signIn("elif", "nope");
        }
        assertThat(newService().signIn("elif", "fishes1").status()).isEqualTo(SignInResult.Status.LOCKED);
    }

    // ---------------------------------------------------------------- 4. remember me

    @Test
    void rememberMe() {
        Account elif = service.create(draft("elif"));
        Account can = service.create(draft("can"));
        assertThat(service.remembered()).isEmpty();

        service.remember(elif.id());
        assertThat(newService().remembered()).map(Account::username).contains("elif");

        service.remember(can.id());
        assertThat(newService().remembered()).map(Account::username).contains("can");

        service.forget();
        assertThat(newService().remembered()).isEmpty();
        service.forget();
        assertThatThrownBy(() -> service.remember("missing")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------- 5. separate folders

    @Test
    void everyAccountHasItsOwnFolder() {
        Account elif = service.create(draft("elif"));
        Account can = service.create(draft("can"));
        assertThat(service.folderOf(elif)).isNotEqualTo(service.folderOf(can));
        assertThat(newService().all()).extracting(Account::username).containsExactly("elif", "can");
    }

    // ---------------------------------------------------------------- 6. old save, 7. no PIN

    @Test
    void theFirstAccountTakesOverTheOldSaveWithItsPin() throws IOException {
        Files.writeString(home.resolve("save.json"), "{\"pin\":\"old\"}");
        Files.writeString(home.resolve("save.json.bak"), "backup");
        Files.createDirectories(home.resolve("dev-checkpoint"));
        Files.writeString(home.resolve("dev-checkpoint").resolve("save.json"), "checkpoint");

        Account first = service.create(draft("elif"));
        Path folder = service.folderOf(first);
        assertThat(Files.readString(folder.resolve("save.json"))).isEqualTo("{\"pin\":\"old\"}");
        assertThat(folder.resolve("save.json.bak")).exists();
        assertThat(folder.resolve("dev-checkpoint").resolve("save.json")).exists();
        assertThat(home.resolve("save.json")).doesNotExist();

        Account second = service.create(draft("can"));
        assertThat(service.folderOf(second).resolve("save.json")).doesNotExist();
    }

    @Test
    void aNewAccountStartsWithoutASaveAndSoWithoutAPin() {
        Account account = service.create(draft("elif"));
        assertThat(service.folderOf(account).resolve("save.json")).doesNotExist();
    }

    // ---------------------------------------------------------------- 10. safety color

    @Test
    void rightSafetyColorSetsANewPassword() {
        Account elif = service.create(draft("elif"));
        assertThat(service.checkSafetyColor(elif.id(), SafetyColor.BLUE).status()).isEqualTo(ResetResult.Status.OK);

        ResetResult reset = service.resetPassword(elif.id(), SafetyColor.BLUE, "corals9", "corals9");
        assertThat(reset.status()).isEqualTo(ResetResult.Status.OK);
        assertThat(reset.account()).isPresent();
        assertThat(service.signIn("elif", "fishes1").status()).isEqualTo(SignInResult.Status.WRONG);
        assertThat(service.signIn("elif", "corals9").status()).isEqualTo(SignInResult.Status.OK);
    }

    @Test
    void wrongSafetyColorChangesNothing() {
        Account elif = service.create(draft("elif"));
        assertThat(service.resetPassword(elif.id(), SafetyColor.RED, "corals9", "corals9").status())
                .isEqualTo(ResetResult.Status.WRONG_COLOR);
        assertThat(service.checkSafetyColor(elif.id(), null).status()).isEqualTo(ResetResult.Status.WRONG_COLOR);
        assertThat(service.signIn("elif", "fishes1").status()).isEqualTo(SignInResult.Status.OK);
    }

    @Test
    void anInvalidNewPasswordIsRefused() {
        Account elif = service.create(draft("elif"));
        assertThat(service.resetPassword(elif.id(), SafetyColor.BLUE, "short", "short").status())
                .isEqualTo(ResetResult.Status.INVALID_PASSWORD);
        assertThat(service.resetPassword(elif.id(), SafetyColor.BLUE, "corals9", "corals8").status())
                .isEqualTo(ResetResult.Status.INVALID_PASSWORD);
        assertThat(service.signIn("elif", "fishes1").status()).isEqualTo(SignInResult.Status.OK);
    }

    @Test
    void threeWrongColorsLockWithWaitsOfOneFiveFifteenSixtyMinutes() {
        Account elif = service.create(draft("elif"));
        long[] expected = {60_000L, 300_000L, 900_000L, 3_600_000L, 3_600_000L};
        for (long wait : expected) {
            assertThat(service.checkSafetyColor(elif.id(), SafetyColor.RED).status()).isEqualTo(ResetResult.Status.WRONG_COLOR);
            assertThat(service.checkSafetyColor(elif.id(), SafetyColor.RED).status()).isEqualTo(ResetResult.Status.WRONG_COLOR);
            ResetResult third = service.checkSafetyColor(elif.id(), SafetyColor.RED);
            assertThat(third.status()).isEqualTo(ResetResult.Status.LOCKED);
            assertThat(third.lockedRemainingMillis()).isEqualTo(wait);
            // The right color does not help while locked.
            assertThat(service.checkSafetyColor(elif.id(), SafetyColor.BLUE).status()).isEqualTo(ResetResult.Status.LOCKED);
            clock.advanceMillis(wait);
        }
    }

    @Test
    void aSuccessfulResetClearsBothLocks() {
        Account elif = service.create(draft("elif"));
        for (int i = 0; i < 5; i++) {
            service.signIn("elif", "nope");
        }
        service.checkSafetyColor(elif.id(), SafetyColor.RED);
        assertThat(service.resetPassword(elif.id(), SafetyColor.BLUE, "corals9", "corals9").status())
                .isEqualTo(ResetResult.Status.OK);
        Account after = service.find(elif.id()).orElseThrow();
        assertThat(after.signIn()).isEqualTo(Lockout.NONE);
        assertThat(after.safety()).isEqualTo(Lockout.NONE);
        assertThat(service.signIn("elif", "corals9").status()).isEqualTo(SignInResult.Status.OK);
    }

    @Test
    void resetOfAnUnknownAccountIsAProgrammingError() {
        assertThatThrownBy(() -> service.checkSafetyColor("missing", SafetyColor.RED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ageOnCountsFullYears() {
        assertThat(AccountService.ageOn(LocalDate.of(2017, 5, 3), LocalDate.of(2026, 5, 2))).isEqualTo(8);
        assertThat(AccountService.ageOn(LocalDate.of(2017, 5, 3), LocalDate.of(2026, 5, 3))).isEqualTo(9);
        assertThat(service.config()).isEqualTo(CONFIG);
    }

    // ---------------------------------------------------------------- profile edits

    @Test
    void namesCanBeChanged() {
        Account elif = service.create(draft("elif"));
        assertThat(service.changeNames(elif.id(), " Elif Su ", "Kaya")).isEqualTo(EditResult.OK);
        assertThat(newService().find(elif.id()).orElseThrow().firstName()).isEqualTo("Elif Su");
        assertThat(service.changeNames(elif.id(), "", "Kaya")).isEqualTo(EditResult.INVALID);
        assertThat(service.find(elif.id()).orElseThrow().lastName()).isEqualTo("Kaya");
    }

    @Test
    void pictureCanBeChanged() {
        Account elif = service.create(draft("elif"));
        service.changePicture(elif.id(), new Picture(Picture.Kind.PHOTO, ""));
        assertThat(newService().find(elif.id()).orElseThrow().picture().kind()).isEqualTo(Picture.Kind.PHOTO);
    }

    @Test
    void passwordChangeNeedsTheCurrentPassword() {
        Account elif = service.create(draft("elif"));
        assertThat(service.changePassword(elif.id(), "wrong1", "corals9", "corals9")).isEqualTo(EditResult.WRONG_PASSWORD);
        assertThat(service.changePassword(elif.id(), "fishes1", "short", "short")).isEqualTo(EditResult.INVALID);
        assertThat(service.changePassword(elif.id(), "fishes1", "corals9", "corals9")).isEqualTo(EditResult.OK);
        assertThat(service.signIn("elif", "corals9").status()).isEqualTo(SignInResult.Status.OK);
    }

    @Test
    void safetyColorChangeNeedsTheCurrentPassword() {
        Account elif = service.create(draft("elif"));
        assertThat(service.changeSafetyColor(elif.id(), "wrong1", SafetyColor.RED)).isEqualTo(EditResult.WRONG_PASSWORD);
        assertThat(service.changeSafetyColor(elif.id(), "fishes1", null)).isEqualTo(EditResult.INVALID);
        assertThat(service.changeSafetyColor(elif.id(), "fishes1", SafetyColor.RED)).isEqualTo(EditResult.OK);
        assertThat(service.checkSafetyColor(elif.id(), SafetyColor.RED).status()).isEqualTo(ResetResult.Status.OK);
        assertThat(service.checkSafetyColor(elif.id(), SafetyColor.BLUE).status()).isEqualTo(ResetResult.Status.WRONG_COLOR);
    }

    // ---------------------------------------------------------------- 8. birthday

    @Test
    void birthdayIncludingTheTwentyNinthOfFebruary() {
        LocalDate may3 = LocalDate.of(2017, 5, 3);
        assertThat(AccountService.isBirthday(may3, LocalDate.of(2026, 5, 3))).isTrue();
        assertThat(AccountService.isBirthday(may3, LocalDate.of(2026, 5, 4))).isFalse();
        LocalDate leap = LocalDate.of(2016, 2, 29);
        assertThat(AccountService.isBirthday(leap, LocalDate.of(2027, 2, 28))).isTrue();
        assertThat(AccountService.isBirthday(leap, LocalDate.of(2027, 3, 1))).isFalse();
        assertThat(AccountService.isBirthday(leap, LocalDate.of(2028, 2, 28))).isFalse();
        assertThat(AccountService.isBirthday(leap, LocalDate.of(2028, 2, 29))).isTrue();
    }

    @Test
    void developerToolsCanPretendAnotherDate() {
        LocalDate real = service.today();
        assertThat(service.isTodayPretended()).isFalse();

        service.setTodayForDevelopment(LocalDate.of(2027, 5, 3));
        assertThat(service.today()).isEqualTo(LocalDate.of(2027, 5, 3));
        assertThat(service.realToday()).isEqualTo(real);
        assertThat(service.isTodayPretended()).isTrue();

        service.setTodayForDevelopment(null);
        assertThat(service.today()).isEqualTo(real);
        assertThat(service.isTodayPretended()).isFalse();
    }
}
