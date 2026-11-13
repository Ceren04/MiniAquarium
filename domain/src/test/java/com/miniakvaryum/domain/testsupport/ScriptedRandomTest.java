package com.miniakvaryum.domain.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ScriptedRandomTest {

    @Test
    void returnsScriptedValuesInOrder() {
        ScriptedRandom random = new ScriptedRandom().ints(3, 0).doubles(0.25, 0.9);

        assertThat(random.nextInt(5)).isEqualTo(3);
        assertThat(random.nextDouble()).isEqualTo(0.25);
        assertThat(random.nextInt(5)).isEqualTo(0);
        assertThat(random.nextDouble()).isEqualTo(0.9);
    }

    @Test
    void failsWhenTheScriptRunsOut() {
        ScriptedRandom random = new ScriptedRandom();

        assertThatThrownBy(() -> random.nextInt(5)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(random::nextDouble).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsWhenAScriptedIntDoesNotFitTheBound() {
        ScriptedRandom random = new ScriptedRandom().ints(5);

        assertThatThrownBy(() -> random.nextInt(5)).isInstanceOf(IllegalStateException.class);
    }
}
