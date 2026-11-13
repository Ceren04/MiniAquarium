package com.miniakvaryum.domain.core.util;

import java.util.UUID;

/** Production {@link IdGenerator}: every id is a random UUID string. */
public final class UuidIdGenerator implements IdGenerator {

    @Override
    public String next() {
        return UUID.randomUUID().toString();
    }
}
