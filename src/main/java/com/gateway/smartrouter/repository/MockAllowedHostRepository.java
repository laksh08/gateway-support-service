package com.gateway.smartrouter.repository;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Mock DB2 repository returning a fixed set of allowed hosts for development and testing.
 */
@Repository
public class MockAllowedHostRepository implements AllowedHostRepository {

    private static final Set<String> MOCK_ALLOWED_HOSTS = Set.of(
            "api.example.com",
            "gateway.example.com",
            "localhost",
            "127.0.0.1"
    );

    @Override
    public Mono<Set<String>> findAllAllowedHosts() {
        return Mono.just(MOCK_ALLOWED_HOSTS);
    }
}
