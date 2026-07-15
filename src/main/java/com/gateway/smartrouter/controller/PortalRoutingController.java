package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.model.RouteEntry;
import com.gateway.smartrouter.routing.ConsulRoutingProvider;
import com.gateway.smartrouter.routing.RouteTarget;
import com.gateway.smartrouter.routing.RoutingCache;
import com.gateway.smartrouter.routing.RoutingProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Admin API for viewing and managing SOAP → service routing entries.
 *
 * <p>When the active provider is Consul, PUT/DELETE changes are written to Consul KV
 * so they survive pod restarts. When the provider is YAML, changes are applied to the
 * in-memory cache only (restart will reload from YAML).
 */
@RestController
@RequestMapping("/api/portal/routes")
public class PortalRoutingController {

    private final RoutingProvider routingProvider;
    private final RoutingCache routingCache;

    public PortalRoutingController(RoutingProvider routingProvider, RoutingCache routingCache) {
        this.routingProvider = routingProvider;
        this.routingCache = routingCache;
    }

    /** List all currently active routes. */
    @GetMapping
    public Flux<RouteEntry> listRoutes() {
        return Flux.fromIterable(routingProvider.getAllRoutes().entrySet())
                .map(e -> toEntry(e.getKey(), e.getValue()))
                .sort((a, b) -> a.serviceMethod().compareToIgnoreCase(b.serviceMethod()));
    }

    /** Get a single route by service method. */
    @GetMapping("/{serviceMethod}")
    public Mono<RouteEntry> getRoute(@PathVariable String serviceMethod) {
        RouteTarget target = routingCache.resolve(serviceMethod);
        if (target == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Route not found: " + serviceMethod));
        }
        return Mono.just(toEntry(serviceMethod, target));
    }

    /**
     * Create or update a route.
     * Body: {@code { "routeValue": "customer-service/soap/CustomerService" }}
     *
     * <p>For Consul provider, the change is persisted to Consul KV.
     * For YAML provider, only the in-memory cache is updated.
     */
    @PutMapping("/{serviceMethod}")
    public Mono<RouteEntry> upsertRoute(
            @PathVariable String serviceMethod,
            @Valid @RequestBody UpsertRouteRequest request) {

        try {
            RouteTarget target = RouteTarget.parse(request.routeValue());
            Map<String, RouteTarget> updated = new java.util.HashMap<>(routingCache.getRoutes());
            updated.put(serviceMethod, target);
            routingCache.replaceTargets(updated);

            if (routingProvider instanceof ConsulRoutingProvider consulProvider) {
                consulProvider.putRoute(serviceMethod, request.routeValue());
            }

            return Mono.just(toEntry(serviceMethod, target));
        } catch (IllegalArgumentException ex) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()));
        }
    }

    /**
     * Delete a route by service method.
     * For Consul provider, also deletes from Consul KV.
     */
    @DeleteMapping("/{serviceMethod}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRoute(@PathVariable String serviceMethod) {
        if (routingCache.resolve(serviceMethod) == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Route not found: " + serviceMethod));
        }
        Map<String, RouteTarget> updated = new java.util.HashMap<>(routingCache.getRoutes());
        updated.remove(serviceMethod);
        routingCache.replaceTargets(updated);

        if (routingProvider instanceof ConsulRoutingProvider consulProvider) {
            consulProvider.deleteRoute(serviceMethod);
        }
        return Mono.empty();
    }

    /** Force a reload from the backing store (YAML or Consul KV). */
    @PostMapping("/refresh")
    public Mono<Map<String, Object>> refreshRoutes() {
        routingProvider.reload();
        return Mono.fromSupplier(() -> Map.<String, Object>of(
                "status", "refreshed",
                "routeCount", routingCache.getRoutes().size()
        ));
    }

    private RouteEntry toEntry(String serviceMethod, RouteTarget target) {
        return new RouteEntry(
                serviceMethod,
                target.rawValue(),
                target.host(),
                target.upstreamPath() != null ? target.upstreamPath() : "",
                target.port()
        );
    }

    public record UpsertRouteRequest(@NotBlank String routeValue) {
    }
}
