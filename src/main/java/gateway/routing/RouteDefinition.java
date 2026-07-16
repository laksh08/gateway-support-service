package gateway.routing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Immutable routing entry that maps a SOAP operation to a logical service.
 *
 * @param operation  SOAP operation local name (e.g. {@code createCustomer})
 * @param serviceName Consul service name used for discovery (e.g. {@code customer-service})
 * @param targetPath  configured upstream path (e.g. {@code /soap/customer});
 *                    may be overridden by service metadata
 * @param timeout     per-request timeout for calls to this service
 */
public record RouteDefinition(
        @NotBlank String operation,
        @NotBlank String serviceName,
        @NotBlank String targetPath,
        @NotNull Duration timeout) {}
