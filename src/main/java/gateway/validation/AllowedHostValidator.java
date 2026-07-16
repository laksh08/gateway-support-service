package gateway.validation;

import gateway.config.GatewayProperties;
import gateway.exception.HostNotAllowedException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * Validates that the request's {@code Host} header is in the configured allow-list.
 *
 * <p>If {@code gateway.allowed-hosts} contains {@code *}, all hosts are permitted.
 * Host matching is case-insensitive and strips the port suffix if present.
 */
@Component
public class AllowedHostValidator {

    private final List<String> allowedHosts;
    private final boolean allowAll;

    public AllowedHostValidator(GatewayProperties gatewayProperties) {
        List<String> configured = gatewayProperties.allowedHosts();
        this.allowedHosts = configured != null ? configured : List.of();
        this.allowAll = this.allowedHosts.contains("*");
    }

    /**
     * Validates the Host of the incoming request.
     *
     * @return empty {@link Mono} on success; errors with {@link HostNotAllowedException} on failure
     */
    public Mono<Void> validate(ServerRequest request) {
        if (allowAll) {
            return Mono.empty();
        }
        String host = extractHost(request);
        if (isAllowed(host)) {
            return Mono.empty();
        }
        return Mono.error(new HostNotAllowedException(host));
    }

    private String extractHost(ServerRequest request) {
        String hostHeader = request.headers().firstHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            return normalise(hostHeader);
        }
        return normalise(request.uri().getHost());
    }

    private boolean isAllowed(String host) {
        return allowedHosts.stream()
                .map(this::normalise)
                .anyMatch(h -> h.equals(host));
    }

    private String normalise(String host) {
        if (host == null) {
            return "";
        }
        String lower = host.trim().toLowerCase();
        int colon = lower.indexOf(':');
        return colon >= 0 ? lower.substring(0, colon) : lower;
    }
}
