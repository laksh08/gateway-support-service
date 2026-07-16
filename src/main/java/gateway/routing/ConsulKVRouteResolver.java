package gateway.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import gateway.consul.ConsulProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Route resolver backed by Consul Key/Value store.
 *
 * <h3>KV layout</h3>
 * Each route lives under the configured prefix as a JSON value:
 *
 * <pre>
 *   Key:   gateway/routes/createCustomer
 *   Value: {"service":"customer-service","targetPath":"/soap/customer","timeout":"PT30S"}
 * </pre>
 *
 * Routes can be updated at runtime via:
 * <pre>
 *   consul kv put gateway/routes/createCustomer \
 *     '{"service":"customer-service","targetPath":"/soap/customer","timeout":"PT30S"}'
 * </pre>
 *
 * <p>This resolver uses a reactive {@link WebClient} to communicate with Consul
 * and never blocks the calling thread.
 */
public class ConsulKVRouteResolver implements RouteResolver {

    private static final Logger log = LoggerFactory.getLogger(ConsulKVRouteResolver.class);

    private final WebClient consulWebClient;
    private final ConsulProperties consulProperties;
    private final ObjectMapper objectMapper;
    private final String prefix;

    public ConsulKVRouteResolver(
            WebClient consulWebClient,
            ConsulProperties consulProperties,
            ObjectMapper objectMapper) {
        this.consulWebClient = consulWebClient;
        this.consulProperties = consulProperties;
        this.objectMapper = objectMapper;
        this.prefix = consulProperties.kvRoutePrefix();
        if (!this.prefix.endsWith("/")) {
            // normalized
        }
    }

    @Override
    public Mono<RouteDefinition> resolve(String operation) {
        return loadAll().map(routes -> routes.get(operation));
    }

    @Override
    public Mono<Map<String, RouteDefinition>> loadAll() {
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return consulWebClient
                .get()
                .uri("/v1/kv/{prefix}?recurse=true", normalizedPrefix)
                .retrieve()
                .bodyToFlux(ConsulKvEntry.class)
                .collectList()
                .map(entries -> parseEntries(entries, normalizedPrefix))
                .doOnSuccess(m -> log.debug("Loaded {} route(s) from Consul KV", m.size()))
                .onErrorResume(
                        ex -> {
                            log.warn("Failed to load routes from Consul KV: {}", ex.getMessage());
                            return Mono.just(Map.of());
                        });
    }

    @Override
    public Mono<Void> reload() {
        log.info("Reloading routes from Consul KV prefix '{}'", prefix);
        return loadAll().then();
    }

    private Map<String, RouteDefinition> parseEntries(
            List<ConsulKvEntry> entries, String normalizedPrefix) {
        Map<String, RouteDefinition> result = new HashMap<>();
        for (ConsulKvEntry entry : entries) {
            if (entry.key() == null || entry.value() == null) continue;
            String operation = entry.key().substring(normalizedPrefix.length()).trim();
            if (operation.isBlank()) continue;
            try {
                String json =
                        new String(
                                Base64.getDecoder().decode(entry.value()), StandardCharsets.UTF_8);
                KvRouteValue kv = objectMapper.readValue(json, KvRouteValue.class);
                Duration timeout =
                        Optional.ofNullable(kv.timeout())
                                .map(Duration::parse)
                                .orElse(Duration.ofSeconds(30));
                result.put(
                        operation,
                        new RouteDefinition(operation, kv.service(), kv.targetPath(), timeout));
            } catch (Exception ex) {
                log.warn("Skipping invalid KV route for operation '{}': {}", operation, ex.getMessage());
            }
        }
        return Map.copyOf(result);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConsulKvEntry(
            @JsonProperty("Key") String key, @JsonProperty("Value") String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KvRouteValue(String service, String targetPath, String timeout) {}
}
