package com.miniakvaryum.domain.core.account;

/**
 * The ten colors a child can pick as "safety color" when signing up. When the child forgets the
 * password, picking the same color again allows setting a new one.
 *
 * <p>The order is the order on screen (two rows of five). The UI keeps the names and the
 * exact colors; the save stores only a salted hash of {@link #name()}.
 */
public enum SafetyColor {
    RED, ORANGE, YELLOW, GREEN, TURQUOISE, BLUE, PURPLE, PINK, BROWN, GRAY
}
