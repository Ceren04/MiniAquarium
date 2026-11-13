package com.miniakvaryum.domain.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class Vec2Test {

    @Test
    void plusAndMinusWorkPerComponent() {
        Vec2 a = new Vec2(3, 4);
        Vec2 b = new Vec2(1, -2);

        assertThat(a.plus(b)).isEqualTo(new Vec2(4, 2));
        assertThat(a.minus(b)).isEqualTo(new Vec2(2, 6));
    }

    @Test
    void scaleMultipliesBothComponents() {
        assertThat(new Vec2(3, -4).scale(2)).isEqualTo(new Vec2(6, -8));
    }

    @Test
    void lengthAndDistanceUsePythagoras() {
        assertThat(new Vec2(3, 4).length()).isEqualTo(5.0);
        assertThat(new Vec2(1, 1).distanceTo(new Vec2(4, 5))).isEqualTo(5.0);
    }

    @Test
    void normalizedKeepsDirectionWithLengthOne() {
        Vec2 n = new Vec2(0, -7).normalized();

        assertThat(n.x()).isEqualTo(0.0);
        assertThat(n.y()).isEqualTo(-1.0);
        assertThat(new Vec2(3, 4).normalized().length()).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void normalizedZeroVectorStaysZero() {
        assertThat(Vec2.ZERO.normalized()).isEqualTo(Vec2.ZERO);
    }

    @Test
    void operationsDoNotChangeTheOriginal() {
        Vec2 a = new Vec2(1, 2);
        a.plus(new Vec2(5, 5));
        a.scale(10);

        assertThat(a).isEqualTo(new Vec2(1, 2));
    }
}
