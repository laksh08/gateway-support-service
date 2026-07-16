package gateway.routing;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Resolves a SOAP operation name to a {@link RouteDefinition}.
 *
 * <p>Implementations may load routes from YAML configuration, Consul KV, a database, or any other
 * backing store. The gateway never calls this on the hot path without caching.
 */
public interface RouteResolver {

    /**
     * Resolves the route for the given SOAP operation.
     *
     * @param operation SOAP operation local name (e.g. {@code createCustomer})
     * @return a {@link Mono} emitting the {@link RouteDefinition}, or empty if not found
     */
    Mono<RouteDefinition> resolve(String operation);

    /**
     * Returns a snapshot of all currently known routes.
     * Used for cache population and portal display.
     */
    Mono<Map<String, RouteDefinition>> loadAll();

    /** Forces a synchronous reload from the backing store (called by cache refresh). */
    Mono<Void> reload();
}
