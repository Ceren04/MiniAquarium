package com.miniakvaryum.domain.core.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventBusTest {

    private record TaskApproved(String taskId) {
    }

    private record FishFed(String fishId) {
    }

    private final EventBus bus = new EventBus();
    private final List<String> log = new ArrayList<>();

    @Test
    void listenersReceiveEventsOfTheirTypeInSubscriptionOrder() {
        bus.subscribe(TaskApproved.class, e -> log.add("first:" + e.taskId()));
        bus.subscribe(TaskApproved.class, e -> log.add("second:" + e.taskId()));

        bus.publish(new TaskApproved("t1"));

        assertThat(log).containsExactly("first:t1", "second:t1");
    }

    @Test
    void otherEventTypesAreNotDelivered() {
        bus.subscribe(TaskApproved.class, e -> log.add("task"));

        bus.publish(new FishFed("f1"));

        assertThat(log).isEmpty();
    }

    @Test
    void publishingWithoutListenersDoesNothing() {
        bus.publish(new FishFed("f1"));
    }

    @Test
    void eventPublishedInsideAListenerIsDeliveredAfterTheCurrentOne() {
        bus.subscribe(TaskApproved.class, e -> {
            log.add("A sees task");
            bus.publish(new FishFed("f1"));
            log.add("A done");
        });
        bus.subscribe(TaskApproved.class, e -> log.add("B sees task"));
        bus.subscribe(FishFed.class, e -> log.add("C sees fish"));

        bus.publish(new TaskApproved("t1"));

        assertThat(log).containsExactly("A sees task", "A done", "B sees task", "C sees fish");
    }

    @Test
    void listenerMaySubscribeWhileHandlingAnEvent() {
        bus.subscribe(TaskApproved.class, e -> bus.subscribe(TaskApproved.class, late -> log.add("late")));

        bus.publish(new TaskApproved("t1"));
        assertThat(log).isEmpty();

        bus.publish(new TaskApproved("t2"));
        assertThat(log).containsExactly("late");
    }

    @Test
    void busStaysUsableAfterAListenerThrows() {
        bus.subscribe(TaskApproved.class, e -> {
            throw new IllegalStateException("bug in listener");
        });
        bus.subscribe(FishFed.class, e -> log.add("fish"));

        assertThatThrownBy(() -> bus.publish(new TaskApproved("t1"))).isInstanceOf(IllegalStateException.class);
        bus.publish(new FishFed("f1"));

        assertThat(log).containsExactly("fish");
    }

    @Test
    void nullsAreRejected() {
        assertThatThrownBy(() -> bus.publish(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> bus.subscribe(null, e -> { })).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> bus.subscribe(FishFed.class, null)).isInstanceOf(NullPointerException.class);
    }
}
