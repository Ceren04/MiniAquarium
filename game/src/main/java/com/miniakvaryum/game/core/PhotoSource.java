package com.miniakvaryum.game.core;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Where profile photos come from. Each platform gives its own implementation to the game: the
 * desktop opens a file window (and later the webcam), Android will use the system gallery and
 * camera app. The game code only uses this interface, so it needs no platform library.
 */
public interface PhotoSource {

    /** No gallery and no camera; the profile then offers only avatars. */
    PhotoSource NONE = new PhotoSource() {
        @Override
        public boolean hasGallery() {
            return false;
        }

        @Override
        public Optional<Path> pickFromGallery() {
            return Optional.empty();
        }
    };

    /** Returns true if {@link #pickFromGallery()} can be used. */
    boolean hasGallery();

    /**
     * Lets the user pick a picture file (png or jpg). Blocks until the user has picked or
     * cancelled.
     *
     * @return the picked file, or empty if cancelled
     */
    Optional<Path> pickFromGallery();

    /**
     * Returns true if a camera is available. Not yet on the desktop: the webcam library is tried
     * on a real laptop first (plan V12, step 0).
     */
    default boolean hasCamera() {
        return false;
    }
}
