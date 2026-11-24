package com.miniakvaryum.domain.core.account;

import java.util.Objects;

/**
 * The profile picture of an account.
 *
 * @param kind     what kind of picture it is
 * @param avatarId for {@link Kind#AVATAR}: the id from the avatar catalog, for example
 *                 {@code "fish/clown"}; empty for the other kinds
 */
public record Picture(Kind kind, String avatarId) {

    /** Kinds of profile picture. */
    public enum Kind {
        /** No picture: the first letter of the first name in a colored circle. */
        INITIAL,
        /** One of the drawings that come with the game. */
        AVATAR,
        /** A photo from the camera or the gallery, stored as {@code photo.png} in the account folder. */
        PHOTO
    }

    public Picture {
        Objects.requireNonNull(kind, "kind");
        avatarId = avatarId == null ? "" : avatarId;
        if (kind == Kind.AVATAR && avatarId.isBlank()) {
            throw new IllegalArgumentException("An avatar picture needs an avatar id");
        }
        if (kind != Kind.AVATAR && !avatarId.isEmpty()) {
            throw new IllegalArgumentException("Only an avatar picture has an avatar id");
        }
    }

    /** No picture chosen ("Skip"). */
    public static Picture initial() {
        return new Picture(Kind.INITIAL, "");
    }

    /** One of the avatars that come with the game. */
    public static Picture avatar(String avatarId) {
        return new Picture(Kind.AVATAR, avatarId);
    }
}
