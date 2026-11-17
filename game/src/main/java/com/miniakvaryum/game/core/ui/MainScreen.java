package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.time.CreditedGap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The main screen. It is deliberately empty: it only provides the frame described in
 * {@link MainLayout}, and features fill it. No feature needs to edit this class.
 */
public final class MainScreen extends BaseScreen implements MainLayout {

    private static final float BAR_HEIGHT = 80f;
    /** Touch screens report at most this many fingers at once (libGDX pointer indices). */
    private static final int MAX_FINGERS = 20;
    private static final Color BAR_COLOR = new Color(0f, 0.10f, 0.20f, 0.75f);

    private record Layer(int z, Actor actor) {
    }

    private record Status(Label label, Supplier<String> text) {
    }

    private final Group world = new Group();
    private final List<Layer> layers = new ArrayList<>();
    private final Table statusCells = new Table();
    private final Table buttonCells = new Table();
    private final List<Status> statuses = new ArrayList<>();
    private final List<WorldTapHandler> tapHandlers = new ArrayList<>();
    private final List<WorldDragHandler> dragHandlers = new ArrayList<>();
    private final float[] lastFingerX = new float[MAX_FINGERS];
    private final float[] lastFingerY = new float[MAX_FINGERS];
    private final List<Supplier<String>> debugLines = new ArrayList<>();
    private final Label debug;

    /**
     * @param session used for the built-in lines of the debug overlay
     * @param skin    look of the widgets
     */
    public MainScreen(GameSession session, Skin skin) {
        super(skin);

        // 1. Back: the world. Not touchable itself; taps are caught by the layer above.
        world.setTouchable(Touchable.disabled);
        stage.addActor(world);

        // 2. Middle: an invisible actor over the whole screen that receives taps into the water.
        Actor tapArea = new Actor();
        tapArea.setBounds(0, 0, UiConfig.VIEW_WIDTH, UiConfig.VIEW_HEIGHT);
        tapArea.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                rememberFinger(pointer, x, y);
                guarded(() -> dispatchTap(x, y));
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (pointer < 0 || pointer >= MAX_FINGERS) {
                    return;
                }
                float fromX = lastFingerX[pointer];
                float fromY = lastFingerY[pointer];
                rememberFinger(pointer, x, y);
                guarded(() -> dispatchDrag(fromX, fromY, x, y));
            }
        });
        stage.addActor(tapArea);

        // 3. Front: the top bar. Empty parts of it let taps fall through.
        Table bar = new Table();
        bar.setBackground(skin.newDrawable(UiSkin.WHITE, BAR_COLOR));
        bar.setBounds(0, UiConfig.VIEW_HEIGHT - BAR_HEIGHT, UiConfig.VIEW_WIDTH, BAR_HEIGHT);
        bar.pad(0, UiConfig.GAP * 2, 0, UiConfig.GAP);
        bar.add(statusCells).left();
        bar.add().expandX();
        bar.add(buttonCells).right();
        stage.addActor(bar);

        debug = new Label("", skin);
        debug.setAlignment(Align.topLeft);
        debug.setTouchable(Touchable.disabled);
        debug.setColor(Color.BLACK);
        debug.setBounds(UiConfig.GAP * 2, 0, UiConfig.VIEW_WIDTH, UiConfig.VIEW_HEIGHT - BAR_HEIGHT - UiConfig.GAP);
        debug.setVisible(false);
        stage.addActor(debug);

        // F3 toggles the debug overlay. Key events go to the stage root when nothing has focus.
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.F3) {
                    debug.setVisible(!debug.isVisible());
                    return true;
                }
                return false;
            }
        });

        addDebugLine(() -> "FPS: " + Gdx.graphics.getFramesPerSecond());
        addDebugLine(() -> "Game time: " + session.time().nowMillis() + " ms");
        addDebugLine(() -> {
            CreditedGap gap = session.creditedGap();
            return "Credited gap: " + gap.millis() + " ms, source " + gap.source() + ", capped " + gap.capped();
        });
    }

    private void rememberFinger(int pointer, float x, float y) {
        if (pointer >= 0 && pointer < MAX_FINGERS) {
            lastFingerX[pointer] = x;
            lastFingerY[pointer] = y;
        }
    }

    private void dispatchDrag(float fromX, float fromY, float toX, float toY) {
        for (WorldDragHandler handler : dragHandlers) {
            if (handler.onDrag(fromX, fromY, toX, toY)) {
                return;
            }
        }
    }

    private void dispatchTap(float x, float y) {
        for (WorldTapHandler handler : tapHandlers) {
            if (handler.onTap(x, y)) {
                return;
            }
        }
    }

    @Override
    protected void update(float delta) {
        for (Status status : statuses) {
            status.label().setText(status.text().get());
        }
        if (debug.isVisible()) {
            StringBuilder text = new StringBuilder();
            for (Supplier<String> line : debugLines) {
                text.append(line.get()).append('\n');
            }
            debug.setText(text);
        }
    }

    @Override
    protected Color background() {
        return Color.BLACK;
    }

    // ------------------------------------------------------------ MainLayout

    @Override
    public void addWorldLayer(int z, Actor actor) {
        // Keep the children of the world group sorted by z; equal z keeps registration order.
        int index = 0;
        while (index < layers.size() && layers.get(index).z() <= z) {
            index++;
        }
        layers.add(index, new Layer(z, actor));
        world.addActorAt(index, actor);
    }

    @Override
    public void addStatus(Supplier<String> text) {
        Label label = new Label(text.get(), skin);
        statuses.add(new Status(label, text));
        statusCells.add(label).padRight(UiConfig.GAP * 3);
    }

    @Override
    public TextButton addButton(String label, Runnable onClick) {
        TextButton button = button(label, onClick);
        buttonCells.add(button).minSize(UiConfig.MIN_TOUCH_SIZE).padLeft(UiConfig.GAP);
        return button;
    }

    @Override
    public TextButton addToggle(String label, Consumer<Boolean> onToggle) {
        TextButton[] holder = new TextButton[1];
        holder[0] = button(label, UiSkin.TOGGLE, () -> onToggle.accept(holder[0].isChecked()));
        buttonCells.add(holder[0]).minSize(UiConfig.MIN_TOUCH_SIZE).padLeft(UiConfig.GAP);
        return holder[0];
    }

    @Override
    public void addWorldTapHandler(WorldTapHandler handler) {
        tapHandlers.add(handler);
    }

    @Override
    public void addWorldDragHandler(WorldDragHandler handler) {
        dragHandlers.add(handler);
    }

    @Override
    public void addDebugLine(Supplier<String> line) {
        debugLines.add(line);
    }
}
