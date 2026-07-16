package gateway.actuator;

import gateway.consul.ConsulClient;
import gateway.discovery.ConsulDiscoveryService;
import gateway.routing.RouteResolver;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Custom health indicator that reports:
 * <ul>
 *   <li>Consul base URL
 *   <li>Number of loaded routes
 *   <li>Number of cached service instances across all services
 * </ul>
 */
@Component("gateway")
public class GatewayHealthIndicator extends AbstractReactiveHealthIndicator {

    private final ConsulClient consulClient;
    private final RouteResolver routeResolver;
    private final ConsulDiscoveryService discoveryService;

    public GatewayHealthIndicator(
            ConsulClient consulClient,
            RouteResolver routeResolver,
            ConsulDiscoveryService discoveryService) {
        this.consulClient = consulClient;
        this.routeResolver = routeResolver;
        this.discoveryService = discoveryService;
    }

    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder) {
        return routeResolver
                .loadAll()
                .map(routes -> {
                    var cachedInstances = discoveryService.getCachedSnapshot();
                    int totalInstances =
                            cachedInstances.values().stream().mapToInt(java.util.List::size).sum();

                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("consul", consulClient.baseUrl());
                    details.put("loadedRoutes", routes.size());
                    details.put("cachedServices", cachedInstances.size());
                    details.put("cachedInstances", totalInstances);

                    return builder.up().withDetails(details).build();
                })
                .onErrorResume(ex -> Mono.just(
                        builder.down(ex)
                                .withDetail("consul", consulClient.baseUrl())
                                .build()));
    }
}
