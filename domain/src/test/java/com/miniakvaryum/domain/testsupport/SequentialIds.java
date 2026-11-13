package com.miniakvaryum.domain.testsupport;

import com.miniakvaryum.domain.core.util.IdGenerator;

/** An {@link IdGenerator} that returns "id-1", "id-2", ... so tests can predict ids. */
public final class SequentialIds implements IdGenerator {

    private int counter;

    @Override
    public String next() {
        counter++;
        return "id-" + counter;
    }
}
