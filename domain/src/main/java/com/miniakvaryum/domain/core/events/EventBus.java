package com.miniakvaryum.domain.core.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lets features announce that something happened without knowing who is interested.
 *
 * <p>An event is a small immutable object, normally a record declared in the {@code api}
 * package of the feature that publishes it, for example {@code TaskApproved(String taskId)}.
 * Any other feature can subscribe to that type and react. The publisher never changes when a
 * new listener appears or an old one is removed.
 *
 * <p>Rule of thumb: if you <b>need</b> something from another feature, call its service.
 * If you only <b>tell</b> others what happened, publish an event.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Delivery is synchronous and single-threaded: {@link #publish} returns after every
 *       listener has run.</li>
 *   <li>A listener receives exactly the event class it subscribed to (no subclass matching).</li>
 *   <li>Listeners run in the order in which they subscribed.</li>
 *   <li>If a listener publishes another event, that event is delivered after the current one
 *       has reached all its listeners. Events are therefore always handled in publish order.</li>
 * </ul>
 */
public final class EventBus {

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();
    private final Deque<Object> queue = new ArrayDeque<>();
    private boolean dispatching;

    /**
     * Registers a listener for one event type. Listeners stay registered for the whole session.
     *
     * @param type     the exact class of the events to receive
     * @param listener called once for every published event of that class
     */
    public <E> void subscribe(Class<E> type, Consumer<? super E> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        listeners.computeIfAbsent(type, t -> new ArrayList<>())
                .add(event -> listener.accept(type.cast(event)));
    }

    /**
     * Delivers the event to every listener of its class. Publishing an event nobody listens to
     * is fine and does nothing.
     */
    public void publish(Object event) {
        queue.add(Objects.requireNonNull(event, "event"));
        if (dispatching) {
            // Called from inside a listener: the loop below will pick the event up.
            return;
        }
        dispatching = true;
        try {
            while (!queue.isEmpty()) {
                Object next = queue.poll();
                // Copy, so a listener may subscribe new listeners while we iterate.
                for (Consumer<Object> listener : List.copyOf(listeners.getOrDefault(next.getClass(), List.of()))) {
                    listener.accept(next);
                }
            }
        } finally {
            // Also runs if a listener throws: leave the bus in a clean state.
            queue.clear();
            dispatching = false;
        }
    }
}
