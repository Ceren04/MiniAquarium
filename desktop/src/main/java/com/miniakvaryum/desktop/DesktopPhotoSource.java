package com.miniakvaryum.desktop;

import com.miniakvaryum.game.core.PhotoSource;
import java.awt.FileDialog;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Photos on the desktop: the gallery is the system file window (png and jpg). A webcam follows
 * after the camera test on a real laptop (plan V12, step 0); until then {@link #hasCamera()} is
 * false.
 */
final class DesktopPhotoSource implements PhotoSource {

    private static final String TITLE = "Choose a picture";

    @Override
    public boolean hasGallery() {
        return !java.awt.GraphicsEnvironment.isHeadless();
    }

    @Override
    public Optional<Path> pickFromGallery() {
        Frame owner = new Frame();
        try {
            FileDialog dialog = new FileDialog(owner, TITLE, FileDialog.LOAD);
            dialog.setFilenameFilter((dir, name) -> isPicture(name));
            // Windows ignores the filter above; this pattern does the same there.
            dialog.setFile("*.png;*.jpg;*.jpeg");
            dialog.setVisible(true);
            if (dialog.getFile() == null || !isPicture(dialog.getFile())) {
                return Optional.empty();
            }
            return Optional.of(Path.of(dialog.getDirectory(), dialog.getFile()));
        } finally {
            owner.dispose();
        }
    }

    private static boolean isPicture(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }
}
