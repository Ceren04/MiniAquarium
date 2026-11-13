package com.miniakvaryum.domain.testsupport;

import com.miniakvaryum.domain.core.time.BootIdProvider;
import java.util.Optional;

/** A {@link BootIdProvider} with a boot id chosen by the test. */
public final class FakeBootId implements BootIdProvider {

    private String bootId;

    private FakeBootId(String bootId) {
        this.bootId = bootId;
    }

    /** A platform that reports the given boot id. */
    public static FakeBootId of(String bootId) {
        return new FakeBootId(bootId);
    }

    /** A platform that cannot report a boot id (for example Windows). */
    public static FakeBootId unknown() {
        return new FakeBootId(null);
    }

    /** Simulates a reboot. */
    public void set(String bootId) {
        this.bootId = bootId;
    }

    @Override
    public Optional<String> currentBootId() {
        return Optional.ofNullable(bootId);
    }
}
