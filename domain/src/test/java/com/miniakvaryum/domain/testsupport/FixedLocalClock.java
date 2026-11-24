package com.miniakvaryum.domain.testsupport;

import com.miniakvaryum.domain.core.time.LocalClock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** A {@link LocalClock} that stands still until a test moves it. */
public final class FixedLocalClock implements LocalClock {

    private LocalDateTime now;

    public FixedLocalClock(LocalDateTime start) {
        this.now = Objects.requireNonNull(start, "start");
    }

    @Override
    public LocalDateTime now() {
        return now;
    }

    public void set(LocalDateTime time) {
        now = Objects.requireNonNull(time, "time");
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }
}
