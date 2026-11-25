package com.miniakvaryum.game.feature.profile;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.AccountService;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.profile.api.ProfileService;
import com.miniakvaryum.domain.feature.stats.api.Stats;
import com.miniakvaryum.domain.feature.stats.api.StatsService;
import com.miniakvaryum.domain.feature.trophies.api.TrophyService;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.account.AccountScreens;
import com.miniakvaryum.game.core.ui.BaseScreen;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.List;
import java.util.Optional;

/**
 * The profile: picture, name and @username (on the birthday with a frame around the picture and
 * "Happy birthday!"), the favorite fish swimming in its box with the other fish below to pick
 * from, "My stats" (the trophy tile opens the trophy road), and the buttons "Edit profile" and
 * "Sign out". It is a child area: no PIN.
 */
final class ProfileScreen extends BaseScreen {

    /** Screen id. */
    static final String ID = "profile";

    /** Screen id of the trophy road (feature "trophies"); UI features open each other's screens by id. */
    private static final String TROPHY_ROAD = "trophies";
    private static final float PICTURE = 150f;
    private static final float FISH_BOX_WIDTH = 420f;
    private static final float FISH_BOX_HEIGHT = 150f;
    private static final float SMALL_FISH = 70f;
    private static final float TILE_WIDTH = 158f;
    private static final float TILE_HEIGHT = 92f;
    private static final float BUTTON_WIDTH = 220f;
    private static final Color BACKGROUND = new Color(0.05f, 0.24f, 0.38f, 1f);
    private static final Color WATER = new Color(0.55f, 0.80f, 0.95f, 1f);
    private static final Color TILE = new Color(1f, 1f, 1f, 0.12f);
    private static final Color GOLD = Color.valueOf("F2B84B");
    private static final Color HINT = new Color(1f, 1f, 1f, 0.7f);
    /** Absolute font scale of small texts (the normal text is 1.6). */
    private static final float SMALL_TEXT = 1.2f;

    private final UiContext ui;
    private final ProfileService profile;
    private final StatsService stats;
    private final AquariumWorld world;
    private final TrophyService trophies;
    private final Table content = new Table();
    /** The birthday frame, or null when the image file is missing. Owned by {@code ui.shapes()}. */
    private final Texture birthdayFrame;

    ProfileScreen(UiContext ui) {
        super(ui.skin());
        this.ui = ui;
        this.profile = ui.session().get(ProfileService.class);
        this.stats = ui.session().get(StatsService.class);
        this.world = ui.session().get(AquariumWorld.class);
        this.trophies = ui.session().get(TrophyService.class);
        this.birthdayFrame = ui.shapes().fileOr(BirthdayFrame.FILE, () -> null);
        Table root = newRootTable();
        root.add(content).grow();
    }

    @Override
    protected Color background() {
        return BACKGROUND;
    }

    @Override
    protected void onShow() {
        rebuild();
    }

    private void rebuild() {
        content.clearChildren();
        content.top();
        content.add(person()).top().left().expandX();
        content.add(favoriteFish()).top().right().row();
        content.add(statsTiles()).colspan(2).padTop(UiConfig.GAP * 2).row();
        Table buttons = new Table();
        buttons.add(button(ProfileStrings.BACK, ui.nav()::showMain)).size(BUTTON_WIDTH * 0.8f, UiConfig.MIN_TOUCH_SIZE);
        buttons.add().expandX();
        buttons.add(button(ProfileStrings.EDIT_PROFILE, () -> ui.nav().show(AccountScreens.EDIT_PROFILE)))
                .size(BUTTON_WIDTH, UiConfig.MIN_TOUCH_SIZE).padRight(UiConfig.GAP);
        buttons.add(button(ProfileStrings.SIGN_OUT, () -> ui.nav().show(AccountScreens.SIGN_OUT)))
                .size(BUTTON_WIDTH * 0.8f, UiConfig.MIN_TOUCH_SIZE);
        content.add(buttons).colspan(2).growX().expandY().bottom();
    }

    /** Picture (tap: change it), birthday greeting, name and @username. */
    private Table person() {
        Account account = ui.account();
        Table person = new Table();
        boolean birthday = AccountService.isBirthday(account.birthday(), ui.accounts().today());
        Actor picture = ui.pictures().of(account, ui.saveFolder(), PICTURE);
        Actor pictureBox = birthday && birthdayFrame != null
                ? new BirthdayFrame(picture, PICTURE, birthdayFrame)
                : picture;
        pictureBox.setTouchable(Touchable.enabled);
        pictureBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ui.nav().show(AccountScreens.PICTURE);
            }
        });
        if (pictureBox instanceof BirthdayFrame) {
            person.add(pictureBox).padRight(UiConfig.GAP);
        } else {
            person.add(pictureBox).size(PICTURE).padRight(UiConfig.GAP * 2);
        }

        Table names = new Table();
        if (birthday) {
            Label greeting = new Label(String.format(ProfileStrings.HAPPY_BIRTHDAY, account.firstName()), skin);
            greeting.setColor(GOLD);
            names.add(greeting).left().row();
        }
        Label name = new Label(account.firstName() + " " + account.lastName(), skin, UiSkin.TITLE);
        names.add(name).left().row();
        Label username = new Label("@" + account.username(), skin);
        username.setColor(HINT);
        names.add(username).left();
        person.add(names).left();
        return person;
    }

    /** The favorite fish swimming in a small aquarium, and the other fish to pick from. */
    private Table favoriteFish() {
        Table box = new Table();
        box.add(new Label(ProfileStrings.MY_FAVORITE_FISH, skin)).left().row();
        Stack aquarium = new Stack();
        aquarium.add(new Image(skin.newDrawable(UiSkin.WHITE, WATER)));
        Optional<FishData> favorite = profile.favorite();
        if (favorite.isPresent()) {
            aquarium.add(new FishPortrait(ui.shapes(), FISH_BOX_WIDTH, FISH_BOX_HEIGHT, 0.8f, true));
        }
        box.add(aquarium).size(FISH_BOX_WIDTH, FISH_BOX_HEIGHT).row();
        List<FishData> all = world.fish().stream().map(FishEntity::toData).toList();
        // Fish levels come with V10; until then every fish is level 1.
        Label line = new Label(favorite.map(f -> String.format(ProfileStrings.FISH_LINE, nameOf(f, all), 1))
                .orElse(ProfileStrings.NO_FISH), skin);
        box.add(line).left().padTop(4).row();

        if (all.size() > 1) {
            Table strip = new Table();
            for (FishData fish : all) {
                boolean chosen = favorite.isPresent() && favorite.get().id().equals(fish.id());
                Stack cell = new Stack();
                cell.add(new Image(skin.newDrawable(UiSkin.WHITE, chosen ? GOLD : TILE)));
                cell.add(new FishPortrait(ui.shapes(), SMALL_FISH, SMALL_FISH * 0.7f, 0.35f, false));
                cell.setTouchable(Touchable.enabled);
                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        profile.choose(fish.id());
                        rebuild();
                    }
                });
                strip.add(cell).size(SMALL_FISH, SMALL_FISH * 0.7f).padRight(6);
            }
            Label hint = new Label(ProfileStrings.TAP_TO_CHOOSE, skin);
            hint.setFontScale(SMALL_TEXT);
            hint.setColor(HINT);
            box.add(hint).left().padTop(6).row();
            box.add(strip).left();
        }
        return box;
    }

    /** The fish's name, or "Fish 1", "Fish 2" ... until fish can be named (V10). */
    private static String nameOf(FishData fish, List<FishData> all) {
        if (fish.name() != null && !fish.name().isBlank()) {
            return fish.name();
        }
        return String.format(ProfileStrings.FISH_NUMBER, all.indexOf(fish) + 1);
    }

    /** "My stats": one tile per number. */
    private Table statsTiles() {
        Stats now = stats.snapshot();
        Table tiles = new Table();
        tiles.add(new Label(ProfileStrings.MY_STATS, skin)).colspan(7).left().padBottom(6).row();
        tile(tiles, now.tasksDone(), ProfileStrings.TASKS_DONE);
        tile(tiles, now.foodGiven(), ProfileStrings.FOOD_GIVEN);
        tile(tiles, now.coinsEarned(), ProfileStrings.COINS_EARNED);
        tile(tiles, now.cleanings(), ProfileStrings.CLEANINGS);
        tile(tiles, now.fishCount(), ProfileStrings.FISH);
        tile(tiles, now.daysPlayed(), ProfileStrings.DAYS_PLAYED);
        Table trophyTile = tile(tiles, String.format(ProfileStrings.TROPHY_COUNT, trophies.claimedCount(),
                trophies.milestones().size()), ProfileStrings.TROPHIES);
        trophyTile.setTouchable(Touchable.enabled);
        trophyTile.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ui.nav().show(TROPHY_ROAD);
            }
        });
        return tiles;
    }

    private Table tile(Table tiles, long value, String label) {
        return tile(tiles, String.valueOf(value), label);
    }

    private Table tile(Table tiles, String value, String label) {
        Table tile = new Table();
        tile.setBackground(skin.newDrawable(UiSkin.WHITE, TILE));
        Label number = new Label(value, skin, UiSkin.TITLE);
        number.setAlignment(Align.center);
        tile.add(number).row();
        Label text = new Label(label, skin);
        text.setFontScale(SMALL_TEXT);
        text.setColor(HINT);
        tile.add(text);
        tiles.add(tile).size(TILE_WIDTH, TILE_HEIGHT).pad(UiConfig.GAP / 2f);
        return tile;
    }
}
