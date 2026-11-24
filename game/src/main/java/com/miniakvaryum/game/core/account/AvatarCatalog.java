package com.miniakvaryum.game.core.account;

import java.util.List;

/**
 * The avatars that come with the game, in rows as on screen. The image of an avatar is
 * {@code assets/avatars/<id>.png}; the id is what the account stores.
 */
public final class AvatarCatalog {

    /** One row of avatars with its title. */
    public record Row(String title, List<String> ids) {
    }

    /** The three rows: Fish, Sea friends, Ocean heroes. */
    public static final List<Row> ROWS = List.of(
            new Row("Fish", List.of("fish/clown", "fish/goldfish", "fish/puffer", "fish/angel", "fish/betta",
                    "fish/ray")),
            new Row("Sea friends", List.of("sea/octopus", "sea/turtle", "sea/starfish", "sea/crab", "sea/seahorse",
                    "sea/dolphin")),
            new Row("Ocean heroes", List.of("heroes/diver", "heroes/mermaid", "heroes/pirate", "heroes/captain",
                    "heroes/princess", "heroes/fisher")));

    private AvatarCatalog() {
    }

    /** Path of the image below the assets folder. */
    public static String imagePath(String avatarId) {
        return "avatars/" + avatarId + ".png";
    }
}
