package com.gateway.smartrouter.service;

import com.gateway.smartrouter.repository.AllowedHostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedHostServiceTest {

    private AllowedHostService allowedHostService;

    @BeforeEach
    void setUp() {
        AllowedHostRepository repository = () -> reactor.core.publisher.Mono.just(
                Set.of("API.Example.COM", "gateway.example.com"));
        allowedHostService = new AllowedHostService(repository);
        allowedHostService.initialize();
    }

    @Test
    void normalizesHostBeforeLookup() {
        assertThat(allowedHostService.isHostAllowed("api.example.com")).isTrue();
        assertThat(allowedHostService.isHostAllowed("API.EXAMPLE.COM:443")).isTrue();
    }

    @Test
    void rejectsUnknownHost() {
        assertThat(allowedHostService.isHostAllowed("evil.example.com")).isFalse();
        assertThat(allowedHostService.isHostAllowed(null)).isFalse();
    }

    @Test
    void reloadReplacesCacheAtomically() {
        StepVerifier.create(allowedHostService.reload())
                .verifyComplete();

        assertThat(allowedHostService.getAllowedHosts()).containsExactlyInAnyOrder(
                "api.example.com", "gateway.example.com");
    }
}
