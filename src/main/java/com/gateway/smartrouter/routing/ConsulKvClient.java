package com.gateway.smartrouter.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gateway.smartrouter.config.ConsulClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reactive HTTP client for Consul KV API.
 *
 * <h3>Consul KV layout</h3>
 * All gateway route keys live under one configurable prefix, e.g. {@code gateway/routes/}.
 * Each key's value is the raw route string: {@code customer-service/soap/CustomerService}.
 *
 * <pre>
 * PUT http://consul:8500/v1/kv/gateway/routes/getCustomer
 * Body: customer-service/soap/CustomerService
 *
 * GET http://consul:8500/v1/kv/gateway/routes/?recurse
 * → [{Key: "gateway/routes/getCustomer", Value: "<base64>"}]
 * </pre>
 */
@Component
public class ConsulKvClient {

    private static final Logger log = LoggerFactory.getLogger(ConsulKvClient.class);

    private final WebClient webClient;
    private final ConsulClientProperties properties;

    public ConsulKvClient(ConsulClientProperties properties) {
        this.properties = properties;
        String baseUrl = String.format("%s://%s:%d", properties.scheme(), properties.host(), properties.port());
        WebClient.Builder builder = WebClient.builder().baseUrl(baseUrl);
        if (properties.token() != null && !properties.token().isBlank()) {
            builder = builder.defaultHeader("X-Consul-Token", properties.token());
        }
        this.webClient = builder.build();
    }

    /**
     * Fetches all key-value pairs under the configured prefix and returns them
     * as a {@code Map<serviceMethod, routeValue>}.
     * The Consul key name after the prefix is used as the service method.
     */
    public Mono<Map<String, String>> fetchRoutes(String keyPrefix) {
        String normalizedPrefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        return webClient.get()
                .uri("/v1/kv/{prefix}?recurse=true", normalizedPrefix)
                .retrieve()
                .bodyToFlux(ConsulKvEntry.class)
                .collectList()
                .map(entries -> parseEntries(entries, normalizedPrefix))
                .onErrorResume(ex -> {
                    log.warn("Consul KV fetch failed for prefix '{}': {} — using empty route map",
                            normalizedPrefix, ex.getMessage());
                    return Mono.just(Map.of());
                });
    }

    /**
     * Writes a single route to Consul KV.
     * Key: {@code {prefix}/{serviceMethod}}, Value: routeValue (e.g. {@code customer-service/soap/Svc}).
     */
    public Mono<Void> putRoute(String keyPrefix, String serviceMethod, String routeValue) {
        String normalizedPrefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        return webClient.put()
                .uri("/v1/kv/{key}", normalizedPrefix + serviceMethod)
                .bodyValue(routeValue)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Written route to Consul KV: {}{} = {}", normalizedPrefix, serviceMethod, routeValue))
                .onErrorMap(ex -> new RuntimeException("Failed to write Consul KV route: " + ex.getMessage(), ex));
    }

    /**
     * Deletes a single route key from Consul KV.
     */
    public Mono<Void> deleteRoute(String keyPrefix, String serviceMethod) {
        String normalizedPrefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        return webClient.delete()
                .uri("/v1/kv/{key}", normalizedPrefix + serviceMethod)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Deleted Consul KV route: {}{}", normalizedPrefix, serviceMethod))
                .onErrorMap(ex -> new RuntimeException("Failed to delete Consul KV route: " + ex.getMessage(), ex));
    }

    private Map<String, String> parseEntries(List<ConsulKvEntry> entries, String prefix) {
        Map<String, String> routes = new HashMap<>();
        for (ConsulKvEntry entry : entries) {
            if (entry.key() == null || entry.value() == null) {
                continue;
            }
            String serviceMethod = entry.key().substring(prefix.length()).trim();
            if (serviceMethod.isBlank()) {
                continue;
            }
            try {
                String decodedValue = new String(
                        Base64.getDecoder().decode(entry.value()), StandardCharsets.UTF_8).trim();
                if (!decodedValue.isBlank()) {
                    routes.put(serviceMethod, decodedValue);
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid Base64 in Consul KV entry '{}': {}", entry.key(), ex.getMessage());
            }
        }
        log.debug("Parsed {} route(s) from Consul KV entries under prefix '{}'", routes.size(), prefix);
        return routes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConsulKvEntry(
            @JsonProperty("Key") String key,
            @JsonProperty("Value") String value
    ) {
    }
}
