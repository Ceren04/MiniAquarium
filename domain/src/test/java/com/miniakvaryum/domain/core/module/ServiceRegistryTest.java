package com.miniakvaryum.domain.core.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ServiceRegistryTest {

    private interface Greeter {
        String greet();
    }

    private final ServiceRegistry registry = new ServiceRegistry();

    @Test
    void providedServiceCanBeRequired() {
        Greeter greeter = () -> "hello";

        registry.provide(Greeter.class, greeter);

        assertThat(registry.require(Greeter.class)).isSameAs(greeter);
        assertThat(registry.find(Greeter.class)).containsSame(greeter);
    }

    @Test
    void missingServiceIsEmptyOrAnExplainedError() {
        assertThat(registry.find(Greeter.class)).isEmpty();
        assertThatThrownBy(() -> registry.require(Greeter.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Greeter")
                .hasMessageContaining("DomainFeatures");
    }

    @Test
    void sameTypeCannotBeProvidedTwice() {
        registry.provide(Greeter.class, () -> "a");

        assertThatThrownBy(() -> registry.provide(Greeter.class, () -> "b"))
                .isInstanceOf(IllegalStateException.class);
    }
}
