package com.miniakvaryum.game.feature.trophies;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.feature.trophies.TrophyConfig;
import com.miniakvaryum.domain.feature.trophies.api.ItemReward;
import com.miniakvaryum.domain.feature.trophies.api.Milestone;
import com.miniakvaryum.game.core.ui.MovingPicture.Motion;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Every item of the road has a picture, an English name and a fitting motion. */
class TrophyPicturesTest {

    private static final Path ASSETS = Path.of(System.getProperty("user.dir")).getParent().resolve("assets");

    @Test
    void everyItemHasAPictureAndAName() {
        for (Milestone m : TrophyConfig.defaults().milestones()) {
            if (m.reward() instanceof ItemReward item) {
                assertThat(ASSETS.resolve("trophies/items/" + item.itemId() + ".png")).as(item.itemId()).exists();
                assertThat(TrophyStrings.itemName(item.itemId())).as(item.itemId()).isNotEqualTo(item.itemId());
            }
        }
        for (String file : new String[] {"trophy", "trophy-locked", "gift-lid", "gift-box-open", "frame-locked",
            "frame-ready", "frame-claimed", "pearl", "bubble", "food", "coins", "items/deco.chest-closed"}) {
            assertThat(Files.exists(ASSETS.resolve("trophies/" + file + ".png"))).as(file).isTrue();
        }
    }

    @Test
    void plantsBendAccessoriesTiltFishSwimTheRestFloats() {
        assertThat(TrophyIcons.motionOf("deco.plant")).isEqualTo(Motion.BEND);
        assertThat(TrophyIcons.motionOf("deco.coral")).isEqualTo(Motion.BEND);
        assertThat(TrophyIcons.motionOf("acc.hat")).isEqualTo(Motion.TILT);
        assertThat(TrophyIcons.motionOf("fish.clown")).isEqualTo(Motion.SWIM);
        assertThat(TrophyIcons.motionOf("deco.shell")).isEqualTo(Motion.BOB);
        assertThat(TrophyIcons.motionOf("part.gold.fin")).isEqualTo(Motion.BOB);
    }

    @Test
    void fishPartsGetSetNames() {
        assertThat(TrophyStrings.itemName("part.rainbow.tail")).isEqualTo("Rainbow tail");
        assertThat(TrophyStrings.itemName("unknown.thing")).isEqualTo("unknown.thing");
        assertThat(TrophyStrings.setName("crystal")).isEqualTo("Crystal");
    }
}
