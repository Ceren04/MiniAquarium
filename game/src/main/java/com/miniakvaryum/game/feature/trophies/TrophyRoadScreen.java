package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.trophies.api.ClaimResult;
import com.miniakvaryum.domain.feature.trophies.api.CurrencyReward;
import com.miniakvaryum.domain.feature.trophies.api.ItemKind;
import com.miniakvaryum.domain.feature.trophies.api.ItemReward;
import com.miniakvaryum.domain.feature.trophies.api.Milestone;
import com.miniakvaryum.domain.feature.trophies.api.Reward;
import com.miniakvaryum.domain.feature.trophies.api.Section;
import com.miniakvaryum.domain.feature.trophies.api.TrophyService;
import com.miniakvaryum.domain.feature.trophies.api.TrophyState;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.BaseScreen;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.List;
import java.util.Optional;

/**
 * The trophy road ("Pokalstrasse"): the section and progress at the top, the road with all 39
 * trophies in the middle (scrolls sideways, starts at the first present or at "You"), "Back" at
 * the bottom. A child area: no PIN.
 */
final class TrophyRoadScreen extends BaseScreen {

    /** Screen id; the profile opens it by this id too. */
    static final String ID = "trophies";

    private static final float CONNECTOR = 90f;
    private static final int DOTS = 4;
    private static final float PEARL = 18f;
    private static final float BUBBLE = 15f;
    private static final float BUBBLE_ALPHA = 0.75f;
    private static final float BUBBLE_RISE = 4f;
    private static final float DIVIDER = 140f;
    private static final float SMALL_TEXT = 1.2f;
    private static final float BUTTON_WIDTH = 176f;
    private static final int SET_SIZE = 4;
    private static final Color BACKGROUND = new Color(0.05f, 0.24f, 0.38f, 1f);
    private static final Color YOU_COLOR = Color.valueOf("FFE08A");
    private static final Color HINT = new Color(1f, 1f, 1f, 0.75f);

    private final UiContext ui;
    private final TrophyService trophies;
    private final TrophyIcons icons;
    private final RewardReveal reveal;
    private final Table content = new Table();

    TrophyRoadScreen(UiContext ui, TrophyIcons icons, RewardReveal reveal) {
        super(ui.skin());
        this.ui = ui;
        this.trophies = ui.session().get(TrophyService.class);
        this.icons = icons;
        this.reveal = reveal;
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
        content.add(new Label(TrophyStrings.TITLE, skin, UiSkin.TITLE)).left().row();
        Label progress = new Label(progressText(), skin);
        progress.setColor(HINT);
        content.add(progress).left().padTop(4).row();

        Table road = new Table();
        Actor focus = buildRoad(road);
        // Start at the first present, or where the child is now (once the road has its size).
        ScrollPane scroll = new ScrollPane(road) {
            private Actor pending = focus;

            @Override
            public void layout() {
                super.layout();
                if (pending != null && getWidth() > 0) {
                    Vector2 at = pending.localToAscendantCoordinates(road, new Vector2(0, 0));
                    scrollTo(at.x, at.y, pending.getWidth(), pending.getHeight(), true, false);
                    updateVisualScroll();
                    pending = null;
                }
            }
        };
        scroll.setScrollingDisabled(false, true);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        content.add(scroll).growX().expandY().padTop(UiConfig.GAP * 2).row();

        Table buttons = new Table();
        buttons.add(button(TrophyStrings.BACK, ui.nav()::back)).size(BUTTON_WIDTH, UiConfig.MIN_TOUCH_SIZE);
        buttons.add().expandX();
        Label collected = new Label(String.format("%s: %d / %d", TrophyStrings.TROPHIES, trophies.claimedCount(),
                trophies.milestones().size()), skin);
        collected.setColor(HINT);
        buttons.add(collected);
        content.add(buttons).growX().bottom();
    }

    /** "Pond - 7 / 8 tasks to the next trophy", or a text for the end of the road. */
    private String progressText() {
        Optional<Milestone> next = trophies.nextMilestone();
        if (next.isPresent()) {
            return String.format(TrophyStrings.PROGRESS, TrophyStrings.section(next.get().section()),
                    trophies.approvedCount(), next.get().threshold());
        }
        return trophies.readyCount() > 0 ? TrophyStrings.ALL_REACHED : TrophyStrings.ALL_COLLECTED;
    }

    /**
     * Adds "You", the trophies, the lines between them and the section signs to the road.
     *
     * @return the actor to scroll to first
     */
    private Actor buildRoad(Table road) {
        int approved = trophies.approvedCount();
        List<Milestone> all = trophies.milestones();
        Actor firstReady = null;
        Actor you = null;
        Section section = null;
        int previous = 0;
        for (Milestone milestone : all) {
            if (milestone.section() != section) {
                section = milestone.section();
                road.add(divider(section)).size(DIVIDER, TrophyTile.HEIGHT);
            }
            Table line = connector(previous, milestone.threshold(), approved);
            if (approved >= previous && approved < milestone.threshold()) {
                you = line;
            }
            road.add(line).size(CONNECTOR, TrophyTile.HEIGHT);
            TrophyState state = trophies.stateOf(milestone.threshold());
            TrophyTile tile = new TrophyTile(skin, icons, milestone, state, approved, () -> open(milestone));
            road.add(tile).size(TrophyTile.WIDTH, TrophyTile.HEIGHT);
            if (state == TrophyState.READY && firstReady == null) {
                firstReady = tile;
            }
            previous = milestone.threshold();
        }
        road.add().width(UiConfig.GAP * 2);
        return firstReady != null ? firstReady : you;
    }

    /** "Part 2 - River" between two sections. */
    private Table divider(Section section) {
        Table divider = new Table();
        Label label = new Label(String.format(TrophyStrings.SECTION, section.ordinal() + 1,
                TrophyStrings.section(section)), skin);
        label.setWrap(true);
        label.setAlignment(Align.center);
        divider.add(label).width(DIVIDER - 16f);
        return divider;
    }

    /**
     * The way to a trophy: pearls for the tasks already done, bubbles for the rest; "You: 9"
     * stands where the pearls end.
     */
    private Table connector(int from, int to, int approved) {
        float done = Math.max(0f, Math.min(1f, (approved - from) / (float) (to - from)));
        int pearls = Math.round(done * DOTS);
        Table connector = new Table();
        if (approved >= from && approved < to) {
            Label you = new Label(String.format(TrophyStrings.YOU, approved), skin);
            you.setFontScale(SMALL_TEXT);
            you.setColor(YOU_COLOR);
            connector.add(you).colspan(DOTS).padBottom(4).row();
        }
        for (int i = 0; i < DOTS; i++) {
            boolean pearl = i < pearls;
            Image dot = new Image(pearl ? icons.pearl() : icons.roadBubble());
            if (!pearl) {
                dot.getColor().a = BUBBLE_ALPHA;
                dot.addAction(Actions.sequence(Actions.delay(i * 0.25f), Actions.forever(Actions.sequence(
                        Actions.moveBy(0f, BUBBLE_RISE, 0.9f, Interpolation.sine),
                        Actions.moveBy(0f, -BUBBLE_RISE, 0.9f, Interpolation.sine)))));
            }
            float size = pearl ? PEARL : BUBBLE;
            connector.add(dot).size(size).pad(0f, 2f, 0f, 2f);
        }
        return connector;
    }

    /** The child taps a present: collect first (the reward is written now), then show the card. */
    private void open(Milestone milestone) {
        if (trophies.claim(milestone.threshold()) != ClaimResult.OK) {
            return;
        }
        Reward reward = milestone.reward();
        String name = reward instanceof ItemReward item ? TrophyStrings.itemName(item.itemId()) : null;
        reveal.show(stage, new RewardReveal.Content(icons.picture(reward), TrophyStrings.revealTitle(reward), name,
                detail(reward)), this::rebuild);
    }

    private String detail(Reward reward) {
        if (reward instanceof CurrencyReward money) {
            return money.currency() == Currency.FOOD ? TrophyStrings.FOOD_DETAIL : TrophyStrings.COINS_DETAIL;
        }
        ItemReward item = (ItemReward) reward;
        if (item.kind() == ItemKind.FISH_PART) {
            String set = item.itemId().split("\\.")[1];
            String prefix = "part." + set + ".";
            long have = trophies.earnedItems().stream().filter(i -> i.itemId().startsWith(prefix)).count();
            String setName = TrophyStrings.setName(set);
            return have >= SET_SIZE ? String.format(TrophyStrings.SET_COMPLETE, setName)
                    : String.format(TrophyStrings.SET_PROGRESS, setName, have, SET_SIZE);
        }
        return TrophyStrings.ITEM_DETAIL;
    }
}
