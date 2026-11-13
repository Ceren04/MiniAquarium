package com.miniakvaryum.desktop;

import com.miniakvaryum.domain.core.time.BootIdProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Boot id on a desktop computer.
 *
 * <p>Linux publishes a random id per boot in a file. Windows and macOS have no equally simple
 * source, so there the id is unknown and the game time falls back to the wall clock with the
 * 24 hour cap.
 */
public final class DesktopBootIdProvider implements BootIdProvider {

    private static final Logger LOG = Logger.getLogger(DesktopBootIdProvider.class.getName());
    private static final Path LINUX_BOOT_ID = Path.of("/proc/sys/kernel/random/boot_id");

    @Override
    public Optional<String> currentBootId() {
        if (!Files.isReadable(LINUX_BOOT_ID)) {
            return Optional.empty();
        }
        try {
            String id = Files.readString(LINUX_BOOT_ID).trim();
            return id.isEmpty() ? Optional.empty() : Optional.of(id);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read the boot id", e);
            return Optional.empty();
        }
    }
}
