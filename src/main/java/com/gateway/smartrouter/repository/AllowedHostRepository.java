package com.gateway.smartrouter.repository;

import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Repository interface for loading allowed hosts from DB2.
 * Production deployments provide a real DB2-backed implementation.
 */
public interface AllowedHostRepository {

    Mono<Set<String>> findAllAllowedHosts();
}
