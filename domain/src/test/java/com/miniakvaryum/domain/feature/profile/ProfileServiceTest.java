package com.miniakvaryum.domain.feature.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.profile.api.ProfileService;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileServiceTest {

    private final List<FishData> fish = new ArrayList<>(List.of(
            new FishData("f1", "Fish 1", 3, 1_000L),
            new FishData("f2", "Fish 2", 0, 9_000L),
            new FishData("f3", "Fish 3", 1, 5_000L)));
    private int saves;

    private ProfileService service(String saved) {
        return new ProfileService(saved, () -> fish, () -> saves++);
    }

    @Test
    void withoutAChoiceTheOldestFishIsTheFavorite() {
        ProfileService profile = service("");
        assertThat(profile.favorite()).map(FishData::id).contains("f2");
        assertThat(profile.isChosen()).isFalse();
    }

    @Test
    void aChosenFishIsTheFavorite() {
        ProfileService profile = service(null);
        profile.choose("f3");
        assertThat(profile.favorite()).map(FishData::id).contains("f3");
        assertThat(profile.isChosen()).isTrue();
        assertThat(saves).isEqualTo(1);
        profile.choose("f3");
        assertThat(saves).isEqualTo(1);
    }

    @Test
    void aChosenFishThatIsGoneFallsBackToTheOldest() {
        ProfileService profile = service("f3");
        fish.removeIf(f -> f.id().equals("f3"));
        assertThat(profile.favorite()).map(FishData::id).contains("f2");
        assertThat(profile.favoriteFishId()).isEqualTo("f3");
    }

    @Test
    void anEmptyAquariumHasNoFavorite() {
        fish.clear();
        assertThat(service("").favorite()).isEmpty();
    }

    @Test
    void onlyFishInTheAquariumCanBeChosen() {
        assertThatThrownBy(() -> service("").choose("nope")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theChoiceIsSaved() {
        TestGame game = new TestGame();
        GameSession session = game.startFullGame();
        ProfileService profile = session.get(ProfileService.class);
        String last = session.get(com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld.class).fish()
                .get(2).id();
        profile.choose(last);
        session.saveNow();
        assertThat(game.startFullGame().get(ProfileService.class).favorite()).map(FishData::id).contains(last);
    }
}
