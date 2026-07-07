package com.gateway.smartrouter.repository;

import com.gateway.smartrouter.model.AllowedHost;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Repository interface for allowed host persistence (DB2 in production).
 */
public interface AllowedHostRepository {

    Flux<AllowedHost> findAll();

    Mono<AllowedHost> findById(Long id);

    Mono<AllowedHost> save(AllowedHost host);

    Mono<Boolean> deleteById(Long id);

    Mono<Set<String>> findActiveHostnames();
}
