package gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Top-level gateway configuration bound from {@code application.yml → gateway:}.
 *
 * <p>All nested records are immutable value objects; they are never mutated after binding.
 */
@Validated
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(
        @DefaultValue("*") List<String> allowedHosts,
        @DefaultValue Map<String, @Valid RouteProperties> routes,
        @DefaultValue("yaml") String routeResolver,
        @DefaultValue("round-robin") String loadBalancer,
        @NotNull @Valid ForwardingProperties forwarding,
        @NotNull @Valid CacheProperties cache) {

    /** Per-operation route configuration. */
    public record RouteProperties(
            @NotBlank String service,
            @NotBlank String targetPath,
            @DefaultValue("PT30S") Duration timeout) {}

    /** WebClient / Reactor Netty connection settings. */
    public record ForwardingProperties(
            @DefaultValue("PT5S") Duration connectTimeout,
            @DefaultValue("PT30S") Duration readTimeout,
            @DefaultValue("PT10S") Duration writeTimeout,
            @DefaultValue("500") @Min(1) int maxConnections,
            @DefaultValue("PT30S") Duration maxIdleTime,
            /** Maximum body size kept in memory per request (bytes). Default 4 MB. */
            @DefaultValue("4194304") @Min(1024) int maxInMemorySizeBytes) {}

    /** Caffeine cache TTLs and sizing. */
    public record CacheProperties(
            @DefaultValue("PT5M") Duration routeTtl,
            @DefaultValue("PT30S") Duration discoveryTtl,
            @DefaultValue("1000") @Min(1) long maxSize) {}
}
