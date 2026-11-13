package com.miniakvaryum.domain.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JavaRandomSourceTest {

    @Test
    void sameSeedGivesSameSequence() {
        JavaRandomSource a = new JavaRandomSource(42);
        JavaRandomSource b = new JavaRandomSource(42);

        for (int i = 0; i < 20; i++) {
            assertThat(a.nextInt(1000)).isEqualTo(b.nextInt(1000));
            assertThat(a.nextDouble()).isEqualTo(b.nextDouble());
        }
    }

    @Test
    void valuesStayInsideTheirRanges() {
        JavaRandomSource random = new JavaRandomSource(7);

        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextInt(5)).isBetween(0, 4);
            assertThat(random.nextDouble()).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
        }
    }

    @Test
    void nonPositiveBoundIsRejected() {
        JavaRandomSource random = new JavaRandomSource(1);

        assertThatThrownBy(() -> random.nextInt(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> random.nextInt(-3)).isInstanceOf(IllegalArgumentException.class);
    }
}
