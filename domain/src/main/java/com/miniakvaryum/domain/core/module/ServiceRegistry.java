package com.miniakvaryum.domain.core.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The list of services that features have made available to each other and to the UI,
 * looked up by their class: {@code registry.require(Wallet.class)}.
 *
 * <p>This replaces fixed fields such as {@code session.wallet}: a new feature adds its service
 * here from its own code, and no shared class has to grow a new field.
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> services = new HashMap<>();

    /**
     * Makes a service available under the given type.
     *
     * @throws IllegalStateException if a service of that type is already registered
     */
    public <T> void provide(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        if (services.putIfAbsent(type, service) != null) {
            throw new IllegalStateException("A service of type " + type.getSimpleName() + " is already provided");
        }
    }

    /** Returns the service of the given type, or empty if no feature provides it. */
    public <T> Optional<T> find(Class<T> type) {
        return Optional.ofNullable(type.cast(services.get(Objects.requireNonNull(type, "type"))));
    }

    /**
     * Returns the service of the given type.
     *
     * @throws IllegalStateException if no feature provides it
     */
    public <T> T require(Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException(
                "No service of type " + type.getSimpleName() + " is provided. "
                        + "Is the feature that provides it listed in DomainFeatures, before the feature that needs it?"));
    }
}
