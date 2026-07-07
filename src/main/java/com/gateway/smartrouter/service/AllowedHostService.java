package com.gateway.smartrouter.service;

import com.gateway.smartrouter.model.AllowedHost;
import com.gateway.smartrouter.model.AllowedHostRequest;
import com.gateway.smartrouter.repository.AllowedHostRepository;
import com.gateway.smartrouter.repository.MockAllowedHostRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages allowed host persistence and the in-memory hostname cache.
 */
@Service
public class AllowedHostService {

    private static final Logger log = LoggerFactory.getLogger(AllowedHostService.class);

    private final AllowedHostRepository allowedHostRepository;
    private final AtomicReference<Set<String>> allowedHosts = new AtomicReference<>(Set.of());

    public AllowedHostService(AllowedHostRepository allowedHostRepository) {
        this.allowedHostRepository = allowedHostRepository;
    }

    @PostConstruct
    public void initialize() {
        reload().subscribe();
    }

    public boolean isHostAllowed(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        return allowedHosts.get().contains(normalizeHost(host));
    }

    public Set<String> getAllowedHosts() {
        return allowedHosts.get();
    }

    public Flux<AllowedHost> findAll() {
        return allowedHostRepository.findAll();
    }

    public Mono<AllowedHost> findById(Long id) {
        return allowedHostRepository.findById(id);
    }

    public Mono<AllowedHost> create(AllowedHostRequest request, String username) {
        Instant now = Instant.now();
        AllowedHost host = new AllowedHost(
                null,
                request.hostname().trim(),
                request.ip(),
                request.description(),
                request.dmzServer(),
                request.status().toUpperCase(),
                username,
                username,
                now,
                now
        );
        return saveNew(host);
    }

    public Mono<AllowedHost> update(Long id, AllowedHostRequest request, String username) {
        return allowedHostRepository.findById(id)
                .flatMap(existing -> {
                    AllowedHost updated = new AllowedHost(
                            existing.id(),
                            request.hostname().trim(),
                            request.ip(),
                            request.description(),
                            request.dmzServer(),
                            request.status().toUpperCase(),
                            existing.createdBy(),
                            username,
                            existing.createdAt(),
                            Instant.now()
                    );
                    if (allowedHostRepository instanceof MockAllowedHostRepository mockRepo) {
                        return mockRepo.update(updated);
                    }
                    return allowedHostRepository.save(updated);
                })
                .flatMap(saved -> reload().thenReturn(saved));
    }

    public Mono<Void> delete(Long id) {
        return allowedHostRepository.deleteById(id)
                .flatMap(deleted -> {
                    if (!deleted) {
                        return Mono.error(new IllegalArgumentException("Host not found: " + id));
                    }
                    return reload();
                });
    }

    public Mono<Void> reload() {
        return allowedHostRepository.findActiveHostnames()
                .doOnNext(hosts -> {
                    Set<String> normalized = hosts.stream()
                            .map(this::normalizeHost)
                            .collect(java.util.stream.Collectors.toUnmodifiableSet());
                    allowedHosts.set(Collections.unmodifiableSet(normalized));
                    log.info("Reloaded allowed host cache with {} active host(s)", normalized.size());
                })
                .then();
    }

    @Scheduled(cron = "${allowed-hosts.reload-cron:0 0 * * * *}")
    public void scheduledReload() {
        reload().subscribe(
                null,
                error -> log.error("Scheduled allowed-host reload failed", error));
    }

    private Mono<AllowedHost> saveNew(AllowedHost host) {
        if (allowedHostRepository instanceof MockAllowedHostRepository mockRepo) {
            return mockRepo.create(host).flatMap(saved -> reload().thenReturn(saved));
        }
        long id = System.currentTimeMillis();
        AllowedHost withId = new AllowedHost(
                id, host.hostname(), host.ip(), host.description(), host.dmzServer(),
                host.status(), host.createdBy(), host.updatedBy(), host.createdAt(), host.updatedAt()
        );
        return allowedHostRepository.save(withId).flatMap(saved -> reload().thenReturn(saved));
    }

    private String normalizeHost(String host) {
        String normalized = host.trim().toLowerCase();
        int colonIndex = normalized.indexOf(':');
        if (colonIndex > 0) {
            normalized = normalized.substring(0, colonIndex);
        }
        return normalized;
    }
}
