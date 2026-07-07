package com.gateway.smartrouter.service;

import com.gateway.smartrouter.repository.AllowedHostRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the in-memory allowed-host cache loaded from DB2.
 * The underlying set is replaced atomically and never mutated in place.
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
        String normalizedHost = normalizeHost(host);
        return allowedHosts.get().contains(normalizedHost);
    }

    public Set<String> getAllowedHosts() {
        return allowedHosts.get();
    }

    public Mono<Void> reload() {
        return allowedHostRepository.findAllAllowedHosts()
                .doOnNext(hosts -> {
                    Set<String> normalized = hosts.stream()
                            .map(this::normalizeHost)
                            .collect(java.util.stream.Collectors.toUnmodifiableSet());
                    allowedHosts.set(Collections.unmodifiableSet(normalized));
                    log.info("Reloaded allowed host cache with {} host(s)", normalized.size());
                })
                .then();
    }

    @Scheduled(cron = "${allowed-hosts.reload-cron:0 0 * * * *}")
    public void scheduledReload() {
        reload().subscribe(
                null,
                error -> log.error("Scheduled allowed-host reload failed", error));
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
