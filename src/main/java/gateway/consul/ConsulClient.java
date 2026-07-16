package gateway.consul;

import gateway.consul.model.ConsulKvEntry;
import gateway.consul.model.ConsulServiceEntry;
import gateway.exception.ConsulCommunicationException;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Reactive HTTP client for the Consul HTTP API.
 *
 * <h3>Endpoints used</h3>
 * <ul>
 *   <li>{@code GET /v1/health/service/{name}?passing=true&dc={dc}}
 *       – healthy instance discovery for the Consul service catalog
 *   <li>{@code GET /v1/kv/{prefix}?recurse=true}
 *       – bulk KV fetch for route definitions (used by {@link gateway.routing.ConsulKVRouteResolver})
 * </ul>
 *
 * <p>All calls are fully reactive; no blocking occurs on the Netty event loop.
 */
@Component
public class ConsulClient {

    private static final Logger log = LoggerFactory.getLogger(ConsulClient.class);

    private static final ParameterizedTypeReference<List<ConsulServiceEntry>> HEALTH_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<List<ConsulKvEntry>> KV_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final ConsulProperties properties;

    public ConsulClient(ConsulProperties properties) {
        this.properties = properties;
        HttpClient httpClient =
                HttpClient.create()
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                (int) properties.connectTimeout().toMillis())
                        .responseTimeout(properties.readTimeout());

        WebClient.Builder builder =
                WebClient.builder()
                        .baseUrl(properties.baseUrl())
                        .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (properties.token() != null && !properties.token().isBlank()) {
            builder = builder.defaultHeader("X-Consul-Token", properties.token());
        }
        this.webClient = builder.build();
    }

    /**
     * Queries Consul for healthy instances of a service.
     *
     * <p>Uses {@code ?passing=true} to return only instances where ALL checks pass,
     * and {@code &dc={datacenter}} to target the correct Consul datacenter.
     *
     * @param serviceName Consul service name (e.g. {@code customer-service})
     * @return Flux of health entries, empty if none passing
     */
    public Flux<ConsulServiceEntry> getHealthyInstances(String serviceName) {
        log.debug("Querying Consul health API for service: {}", serviceName);
        return webClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/v1/health/service/{name}")
                                        .queryParam("passing", "true")
                                        .queryParam("dc", properties.datacenter())
                                        .build(serviceName))
                .retrieve()
                .bodyToFlux(ConsulServiceEntry.class)
                .doOnNext(e -> log.trace("Consul health entry: {}", e.service().id()))
                .onErrorMap(
                        ex -> !(ex instanceof ConsulCommunicationException),
                        ex -> new ConsulCommunicationException(
                                "Consul health query failed for service '"
                                        + serviceName
                                        + "': "
                                        + ex.getMessage(),
                                ex));
    }

    /**
     * Fetches all KV entries under the given prefix using the Consul KV recurse API.
     *
     * @param prefix KV path prefix (e.g. {@code gateway/routes/})
     * @return Mono of KV entries, empty list on 404 (no keys under prefix)
     */
    public Mono<List<ConsulKvEntry>> getKvEntries(String prefix) {
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return webClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/v1/kv/{prefix}")
                                        .queryParam("recurse", "true")
                                        .build(normalizedPrefix))
                .retrieve()
                .bodyToMono(KV_LIST_TYPE)
                .onErrorResume(
                        WebClientResponseException.NotFound.class,
                        ex -> {
                            log.debug("No KV entries found at prefix '{}'", normalizedPrefix);
                            return Mono.just(List.of());
                        })
                .onErrorMap(
                        ex -> !(ex instanceof ConsulCommunicationException),
                        ex -> new ConsulCommunicationException(
                                "Consul KV query failed: " + ex.getMessage(), ex));
    }

    /** Returns the configured base URL for health checks and diagnostics. */
    public String baseUrl() {
        return properties.baseUrl();
    }
}
