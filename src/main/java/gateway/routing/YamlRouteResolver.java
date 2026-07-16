package gateway.routing;

import gateway.config.GatewayProperties;
import gateway.config.GatewayProperties.RouteProperties;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Route resolver that reads routes from {@code application.yml → gateway.routes}.
 *
 * <p>Routes are available immediately at startup and never change unless the application is
 * restarted. Use {@link ConsulKVRouteResolver} for runtime-updateable routes.
 */
public class YamlRouteResolver implements RouteResolver {

    private static final Logger log = LoggerFactory.getLogger(YamlRouteResolver.class);

    private final Map<String, RouteDefinition> routes;

    public YamlRouteResolver(GatewayProperties properties) {
        this.routes = buildRouteMap(properties.routes());
        log.info("Loaded {} route(s) from YAML configuration", routes.size());
        routes.forEach(
                (op, route) ->
                        log.debug("  {} → service={}, path={}", op, route.serviceName(), route.targetPath()));
    }

    @Override
    public Mono<RouteDefinition> resolve(String operation) {
        return Mono.justOrEmpty(routes.get(operation));
    }

    @Override
    public Mono<Map<String, RouteDefinition>> loadAll() {
        return Mono.just(routes);
    }

    @Override
    public Mono<Void> reload() {
        log.debug("YamlRouteResolver: reload is a no-op (routes are static)");
        return Mono.empty();
    }

    private static Map<String, RouteDefinition> buildRouteMap(
            Map<String, RouteProperties> rawRoutes) {
        if (rawRoutes == null || rawRoutes.isEmpty()) {
            return Map.of();
        }
        return rawRoutes.entrySet().stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                e ->
                                        new RouteDefinition(
                                                e.getKey(),
                                                e.getValue().service(),
                                                e.getValue().targetPath(),
                                                e.getValue().timeout())));
    }
}
