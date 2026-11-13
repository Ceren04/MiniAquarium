package com.miniakvaryum.domain.core.save;

/**
 * Thrown when saved data has a newer version than this code supports, for example after
 * a downgrade of the game. The game must stop instead of overwriting data it does not understand.
 */
public final class UnsupportedSaveVersionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedSaveVersionException(int foundVersion, int supportedVersion) {
        super("Save version " + foundVersion + " is newer than the supported version " + supportedVersion);
    }
}
