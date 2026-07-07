package com.gateway.smartrouter.repository;

import com.gateway.smartrouter.model.AllowedHost;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory mock DB2 repository for development and testing.
 */
@Repository
public class MockAllowedHostRepository implements AllowedHostRepository {

    private final Map<Long, AllowedHost> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(3);

    public MockAllowedHostRepository() {
        Instant now = Instant.now();
        store.put(1L, new AllowedHost(1L, "api.example.com", "10.10.1.10",
                "Primary API gateway host", "dmz-web-01", "ACTIVE", "system", "system", now, now));
        store.put(2L, new AllowedHost(2L, "gateway.example.com", "10.10.1.20",
                "External gateway endpoint", "dmz-web-02", "ACTIVE", "system", "system", now, now));
        store.put(3L, new AllowedHost(3L, "localhost", "127.0.0.1",
                "Local development", "dmz-dev-01", "ACTIVE", "system", "system", now, now));
    }

    @Override
    public Flux<AllowedHost> findAll() {
        return Flux.fromIterable(store.values())
                .sort((a, b) -> a.hostname().compareToIgnoreCase(b.hostname()));
    }

    @Override
    public Mono<AllowedHost> findById(Long id) {
        return Mono.justOrEmpty(store.get(id));
    }

    @Override
    public Mono<AllowedHost> save(AllowedHost host) {
        return Mono.fromCallable(() -> {
            store.put(host.id(), host);
            return host;
        });
    }

    @Override
    public Mono<Boolean> deleteById(Long id) {
        return Mono.fromCallable(() -> store.remove(id) != null);
    }

    @Override
    public Mono<Set<String>> findActiveHostnames() {
        return findAll()
                .filter(host -> "ACTIVE".equalsIgnoreCase(host.status()))
                .map(AllowedHost::hostname)
                .collect(Collectors.toSet());
    }

    public Mono<AllowedHost> create(AllowedHost host) {
        return Mono.fromCallable(() -> {
            long id = idSequence.incrementAndGet();
            AllowedHost created = new AllowedHost(
                    id,
                    host.hostname(),
                    host.ip(),
                    host.description(),
                    host.dmzServer(),
                    host.status(),
                    host.createdBy(),
                    host.updatedBy(),
                    host.createdAt(),
                    host.updatedAt()
            );
            store.put(id, created);
            return created;
        });
    }

    public Mono<AllowedHost> update(AllowedHost host) {
        return Mono.fromCallable(() -> {
            if (!store.containsKey(host.id())) {
                return null;
            }
            store.put(host.id(), host);
            return host;
        });
    }
}
