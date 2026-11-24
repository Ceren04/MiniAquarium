package com.miniakvaryum.domain.core.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code accounts.json} in the app folder, and knows where each account keeps
 * its save.
 *
 * <pre>
 * &lt;app folder&gt;/
 *   accounts.json            list of accounts and the remembered one
 *   accounts/&lt;id&gt;/save.json  the save of one account (+ .bak, dev-checkpoint)
 * </pre>
 *
 * <p>Writing works like {@code JsonFileSaveStore}: temporary file, forced to disk, previous file
 * kept as {@code .bak}, then one rename. A damaged file is moved aside (never deleted) and the
 * backup is used.
 */
public final class AccountStore {

    /** Version of the {@code accounts.json} format. */
    public static final int VERSION = 1;

    private static final Logger LOG = Logger.getLogger(AccountStore.class.getName());
    private static final String FILE_NAME = "accounts.json";
    private static final String ACCOUNTS_FOLDER = "accounts";
    /** Files and folders of the save from before accounts existed, moved into the first account. */
    private static final List<String> LEGACY_SAVE_ENTRIES = List.of("save.json", "save.json.bak", "dev-checkpoint");

    private final Path home;
    private final Path file;
    private final Path backup;
    private final Path temp;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

    /**
     * @param home the app folder, normally {@code JsonFileSaveStore.defaultDirectory()}
     */
    public AccountStore(Path home) {
        this.home = Objects.requireNonNull(home, "home");
        this.file = home.resolve(FILE_NAME);
        this.backup = home.resolve(FILE_NAME + ".bak");
        this.temp = home.resolve(FILE_NAME + ".tmp");
    }

    /** Everything in {@code accounts.json}. */
    public record Data(String rememberedId, List<Account> accounts) {
        public Data {
            rememberedId = rememberedId == null ? "" : rememberedId;
            accounts = List.copyOf(accounts);
        }

        /** No accounts yet. */
        public static Data empty() {
            return new Data("", List.of());
        }
    }

    /** The save folder of an account. */
    public Path folderOf(String accountId) {
        return home.resolve(ACCOUNTS_FOLDER).resolve(accountId);
    }

    // ---------------------------------------------------------------- load

    /** Loads the accounts; a missing or unreadable file (and backup) gives no accounts. */
    public Data load() {
        if (!Files.exists(file)) {
            return Data.empty();
        }
        try {
            return read(file);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "accounts.json is damaged, trying the backup", e);
            keepCorruptFile();
        }
        if (Files.exists(backup)) {
            try {
                return read(backup);
            } catch (IOException | RuntimeException e) {
                LOG.log(Level.WARNING, "The backup of accounts.json is damaged too", e);
            }
        }
        return Data.empty();
    }

    private Data read(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        int version = root.get("version").getAsInt();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unknown accounts.json version " + version);
        }
        List<Account> accounts = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("accounts")) {
            accounts.add(decode(element.getAsJsonObject()));
        }
        JsonElement remembered = root.get("remembered");
        return new Data(remembered == null || remembered.isJsonNull() ? "" : remembered.getAsString(), accounts);
    }

    private Account decode(JsonObject json) {
        JsonObject picture = json.getAsJsonObject("picture");
        return new Account(
                json.get("id").getAsString(),
                json.get("username").getAsString(),
                json.get("firstName").getAsString(),
                json.get("lastName").getAsString(),
                LocalDate.parse(json.get("birthday").getAsString()),
                gson.fromJson(json.get("password"), Secret.class),
                gson.fromJson(json.get("safetyColor"), Secret.class),
                new Picture(Picture.Kind.valueOf(picture.get("kind").getAsString()),
                        picture.has("avatarId") ? picture.get("avatarId").getAsString() : ""),
                gson.fromJson(json.get("signIn"), Lockout.class),
                gson.fromJson(json.get("safety"), Lockout.class),
                json.get("createdAtMillis").getAsLong());
    }

    private void keepCorruptFile() {
        try {
            Path target = file.resolveSibling(FILE_NAME + ".corrupt-" + Files.getLastModifiedTime(file).toMillis());
            for (int n = 1; Files.exists(target); n++) {
                target = file.resolveSibling(FILE_NAME + ".corrupt-" + Files.getLastModifiedTime(file).toMillis() + "-" + n);
            }
            Files.move(file, target);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not move the damaged accounts.json aside", e);
        }
    }

    // ---------------------------------------------------------------- save

    /**
     * Writes the accounts atomically.
     *
     * @throws UncheckedIOException if the file could not be written
     */
    public void save(Data data) {
        Objects.requireNonNull(data, "data");
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.addProperty("remembered", data.rememberedId().isEmpty() ? null : data.rememberedId());
        JsonArray list = new JsonArray();
        data.accounts().forEach(account -> list.add(encode(account)));
        root.add("accounts", list);
        byte[] bytes = gson.toJson(root).getBytes(StandardCharsets.UTF_8);
        try {
            Files.createDirectories(home);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            if (Files.exists(file)) {
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
    }

    private JsonObject encode(Account account) {
        JsonObject json = new JsonObject();
        json.addProperty("id", account.id());
        json.addProperty("username", account.username());
        json.addProperty("firstName", account.firstName());
        json.addProperty("lastName", account.lastName());
        json.addProperty("birthday", account.birthday().toString());
        json.add("password", gson.toJsonTree(account.password()));
        json.add("safetyColor", gson.toJsonTree(account.safetyColor()));
        JsonObject picture = new JsonObject();
        picture.addProperty("kind", account.picture().kind().name());
        if (!account.picture().avatarId().isEmpty()) {
            picture.addProperty("avatarId", account.picture().avatarId());
        }
        json.add("picture", picture);
        json.add("signIn", gson.toJsonTree(account.signIn()));
        json.add("safety", gson.toJsonTree(account.safety()));
        json.addProperty("createdAtMillis", account.createdAtMillis());
        return json;
    }

    // ---------------------------------------------------------------- old save

    /** Returns true if a save from before accounts existed lies directly in the app folder. */
    public boolean hasLegacySave() {
        return Files.exists(home.resolve(LEGACY_SAVE_ENTRIES.get(0)));
    }

    /**
     * Moves the save from before accounts existed ({@code save.json}, its backup and the
     * developer checkpoint) into the folder of the given account. Nothing is deleted or copied:
     * the files keep their content, including the parent PIN.
     *
     * @return true if a save was moved
     * @throws UncheckedIOException if moving failed
     */
    public boolean moveLegacySaveTo(String accountId) {
        if (!hasLegacySave()) {
            return false;
        }
        Path target = folderOf(accountId);
        try {
            Files.createDirectories(target);
            for (String name : LEGACY_SAVE_ENTRIES) {
                Path source = home.resolve(name);
                if (Files.exists(source)) {
                    Files.move(source, target.resolve(name));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not move the old save into " + target, e);
        }
        return true;
    }
}
