package com.gateway.smartrouter.service;

import com.gateway.smartrouter.config.EnvoyProperties;
import com.gateway.smartrouter.exception.RouteNotFoundException;
import com.gateway.smartrouter.routing.RouteTarget;
import com.gateway.smartrouter.routing.RoutingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Forwards the original SOAP request unchanged to the downstream service via Envoy.
 *
 * <h3>URL building strategy (per {@link EnvoyProperties#mode()})</h3>
 * <dl>
 *   <dt>consul-dns (default)</dt>
 *   <dd>{@code http://{host}.{consulDomain}:{port}/{upstreamPath}}</dd>
 *   <dt>upstream-port</dt>
 *   <dd>{@code http://{envoyProxyHost}:{port}/{upstreamPath}}
 *       — port comes from the route definition or {@code forwarding.envoy.default-upstream-port}</dd>
 *   <dt>passthrough</dt>
 *   <dd>{@code http://{host}:{port}/{upstreamPath}} — no DNS suffix added</dd>
 * </dl>
 *
 * <h3>Path resolution</h3>
 * <ul>
 *   <li>If the {@link RouteTarget} has an explicit upstream path, that path replaces the original gateway path.</li>
 *   <li>Otherwise the original request path ({@code /WebServices/Gateway/CBISvc}) is forwarded as-is.</li>
 * </ul>
 */
@Service
public class RequestForwardingService {

    private static final Logger log = LoggerFactory.getLogger(RequestForwardingService.class);

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host"
    );

    private static final List<String> TRACING_HEADER_PREFIXES = List.of(
            "x-b3-", "b3", "traceparent", "tracestate", "x-request-id", "x-correlation-id"
    );

    private final WebClient webClient;
    private final EnvoyProperties envoyProperties;
    private final RoutingProvider routingProvider;

    public RequestForwardingService(
            WebClient forwardingWebClient,
            EnvoyProperties envoyProperties,
            RoutingProvider routingProvider) {
        this.webClient = forwardingWebClient;
        this.envoyProperties = envoyProperties;
        this.routingProvider = routingProvider;
    }

    public Mono<ResponseEntity<byte[]>> forward(
            ServerWebExchange exchange,
            String serviceMethod,
            byte[] requestBody) {

        RouteTarget target = routingProvider.resolveTarget(serviceMethod);
        if (target == null) {
            return Mono.error(new RouteNotFoundException(serviceMethod));
        }

        String originalPath = exchange.getRequest().getURI().getRawPath();
        String downstreamUrl = buildDownstreamUrl(target, originalPath);
        HttpMethod method = exchange.getRequest().getMethod();

        log.debug("Forwarding {} {} → {}", serviceMethod, originalPath, downstreamUrl);

        return webClient.method(method)
                .uri(downstreamUrl)
                .headers(headers -> copyRequestHeaders(exchange.getRequest().getHeaders(), headers))
                .body(BodyInserters.fromValue(requestBody))
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .map(body -> ResponseEntity.status(clientResponse.statusCode())
                                .headers(copyResponseHeaders(clientResponse.headers().asHttpHeaders()))
                                .body(body)));
    }

    /**
     * Builds the full downstream URL for an Envoy-aware Consul service mesh.
     *
     * @param target       Parsed route target (host, optional port, optional upstream path)
     * @param originalPath The incoming gateway request path (used when no upstreamPath set)
     * @return Full URL string ready for {@link WebClient}
     */
    String buildDownstreamUrl(RouteTarget target, String originalPath) {
        String host = buildHost(target);
        int port = resolvePort(target);
        String path = target.hasUpstreamPath() ? target.upstreamPath() : originalPath;
        if (path == null) {
            path = "";
        }
        if (!path.startsWith("/") && !path.isBlank()) {
            path = "/" + path;
        }
        return String.format("http://%s:%d%s", host, port, path);
    }

    private String buildHost(RouteTarget target) {
        return switch (envoyProperties.mode()) {
            case EnvoyProperties.MODE_CONSUL_DNS ->
                    target.host() + "." + envoyProperties.consulDomain();
            case EnvoyProperties.MODE_UPSTREAM_PORT ->
                    envoyProperties.envoyProxyHost();
            default -> // passthrough
                    target.host();
        };
    }

    private int resolvePort(RouteTarget target) {
        if (EnvoyProperties.MODE_UPSTREAM_PORT.equals(envoyProperties.mode())) {
            return target.hasPort() ? target.port() : envoyProperties.defaultUpstreamPort();
        }
        return target.hasPort() ? target.port() : envoyProperties.defaultPort();
    }

    private void copyRequestHeaders(HttpHeaders source, HttpHeaders target) {
        source.forEach((name, values) -> {
            String lowerName = name.toLowerCase();
            if (!HOP_BY_HOP_HEADERS.contains(lowerName)) {
                target.addAll(name, values);
            }
        });
        // Ensure tracing headers are propagated even if they were in hop-by-hop
        preserveTracingHeaders(source, target);
    }

    private void preserveTracingHeaders(HttpHeaders source, HttpHeaders target) {
        source.forEach((name, values) -> {
            String lowerName = name.toLowerCase();
            if (TRACING_HEADER_PREFIXES.stream().anyMatch(lowerName::startsWith)) {
                target.put(name, values);
            }
        });
    }

    private HttpHeaders copyResponseHeaders(HttpHeaders source) {
        HttpHeaders result = new HttpHeaders();
        source.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                result.addAll(name, values);
            }
        });
        return result;
    }
}
