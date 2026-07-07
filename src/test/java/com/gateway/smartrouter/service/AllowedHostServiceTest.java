package com.gateway.smartrouter.service;

import com.gateway.smartrouter.repository.MockAllowedHostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedHostServiceTest {

    private AllowedHostService allowedHostService;

    @BeforeEach
    void setUp() {
        allowedHostService = new AllowedHostService(new MockAllowedHostRepository());
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

        assertThat(allowedHostService.getAllowedHosts())
                .contains("api.example.com", "gateway.example.com", "localhost");
    }
}
