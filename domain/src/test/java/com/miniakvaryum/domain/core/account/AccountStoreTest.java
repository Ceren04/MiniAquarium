package com.miniakvaryum.domain.core.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountStoreTest {

    @TempDir
    Path home;

    private static Account account(String id, String username, Picture picture) {
        return new Account(id, username, "Elif", "Yilmaz", LocalDate.of(2017, 5, 3),
                new Secret("c2FsdA==", "aGFzaA==", 1_000), new Secret("c2FsdDI=", "aGFzaDI=", 1_000),
                picture, new Lockout(2, 1, 100L, 200L), Lockout.NONE, 42L);
    }

    @Test
    void writesAndReadsEveryField() {
        AccountStore store = new AccountStore(home);
        Account elif = account("a1", "elif", Picture.avatar("fish/clown"));
        Account can = account("b2", "can", Picture.initial());
        store.save(new AccountStore.Data("b2", List.of(elif, can)));

        AccountStore.Data loaded = new AccountStore(home).load();
        assertThat(loaded.rememberedId()).isEqualTo("b2");
        assertThat(loaded.accounts()).containsExactly(elif, can);
    }

    @Test
    void missingFileMeansNoAccounts() {
        assertThat(new AccountStore(home).load()).isEqualTo(AccountStore.Data.empty());
    }

    @Test
    void aDamagedFileIsKeptAsideAndTheBackupIsUsed() throws IOException {
        AccountStore store = new AccountStore(home);
        store.save(new AccountStore.Data("", List.of(account("a1", "elif", Picture.initial()))));
        store.save(new AccountStore.Data("a1", List.of(account("a1", "elif", Picture.initial()))));
        Files.writeString(home.resolve("accounts.json"), "{ broken");

        AccountStore.Data loaded = new AccountStore(home).load();
        assertThat(loaded.accounts()).extracting(Account::username).containsExactly("elif");
        try (var files = Files.list(home)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .anyMatch(name -> name.startsWith("accounts.json.corrupt-"));
        }
    }

    @Test
    void damagedFileWithoutUsableBackupMeansNoAccounts() throws IOException {
        Files.createDirectories(home);
        Files.writeString(home.resolve("accounts.json"), "{\"version\": 99, \"accounts\": []}");
        Files.writeString(home.resolve("accounts.json.bak"), "not json");
        assertThat(new AccountStore(home).load().accounts()).isEmpty();
    }

    @Test
    void noLegacySaveMeansNothingToMove() {
        AccountStore store = new AccountStore(home);
        assertThat(store.hasLegacySave()).isFalse();
        assertThat(store.moveLegacySaveTo("a1")).isFalse();
    }

    @Test
    void pictureRules() {
        assertThatThrownBy(() -> new Picture(Picture.Kind.AVATAR, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Picture(Picture.Kind.PHOTO, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Picture(Picture.Kind.PHOTO, null).avatarId()).isEmpty();
    }

    @Test
    void lockoutCountsAndReports() {
        Lockout lockout = Lockout.NONE.wrong(1_000L, 2, List.of(10));
        assertThat(lockout.attemptsLeft(2)).isEqualTo(1);
        assertThat(lockout.remainingMillis(1_000L)).isZero();
        lockout = lockout.wrong(1_000L, 2, List.of(10));
        assertThat(lockout.remainingMillis(5_000L)).isEqualTo(6_000L);
        assertThat(lockout.remainingMillis(20_000L)).isZero();
        assertThatThrownBy(() -> new Lockout(-1, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void configChecksItsValues() {
        assertThat(AccountsConfig.defaults().pbkdf2Iterations()).isEqualTo(120_000);
        assertThatThrownBy(() -> new AccountsConfig(30, 3, 2, 6, 64, 3, 17, 5, List.of(1), 3, List.of(1), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountsConfig(30, 3, 15, 6, 64, 3, 17, 5, List.of(), 3, List.of(1), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PasswordHasher(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Secret("a", "b", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
