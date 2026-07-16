package gateway.proxy;

import gateway.discovery.ServiceInstance;
import java.time.Duration;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Proxies an incoming request to a downstream service and returns the original response unchanged.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Forward all headers except hop-by-hop headers.
 *   <li>Stream the original request body without modification.
 *   <li>Return the downstream status, headers, and body unchanged.
 *   <li>Never block the Netty event loop.
 * </ul>
 */
public interface ProxyService {

    /**
     * Proxies the request to the given target URL.
     *
     * @param request        original incoming {@link ServerRequest}
     * @param targetUrl      fully qualified URL built from Consul discovery data
     * @param bodyBytes      raw request body bytes (must be forwarded unchanged)
     * @param instance       selected service instance (used for connection tracking)
     * @param requestTimeout per-operation timeout from route or service metadata
     * @return a {@link Mono} emitting the downstream {@link ServerResponse}
     */
    Mono<ServerResponse> proxy(
            ServerRequest request,
            String targetUrl,
            byte[] bodyBytes,
            ServiceInstance instance,
            Duration requestTimeout);
}
