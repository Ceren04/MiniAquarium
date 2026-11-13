package com.miniakvaryum.domain.core.util;

/**
 * Creates unique ids for new domain objects (tasks, fish, pellets).
 *
 * <p>Behind an interface so tests can use predictable ids ("id-1", "id-2", ...).
 */
public interface IdGenerator {

    /** Returns an id that this generator has never returned before. */
    String next();
}
