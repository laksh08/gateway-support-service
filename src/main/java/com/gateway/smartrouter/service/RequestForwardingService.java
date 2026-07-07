package com.gateway.smartrouter.service;

import com.gateway.smartrouter.config.ForwardingProperties;
import com.gateway.smartrouter.exception.RouteNotFoundException;
import com.gateway.smartrouter.routing.RoutingProvider;
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
 * Forwards the original request unchanged to the resolved downstream service via Envoy.
 */
@Service
public class RequestForwardingService {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host"
    );

    private static final List<String> TRACING_HEADER_PREFIXES = List.of(
            "x-b3-", "b3", "traceparent", "tracestate", "x-request-id", "x-correlation-id"
    );

    private final WebClient webClient;
    private final ForwardingProperties forwardingProperties;
    private final RoutingProvider routingProvider;

    public RequestForwardingService(
            WebClient forwardingWebClient,
            ForwardingProperties forwardingProperties,
            RoutingProvider routingProvider) {
        this.webClient = forwardingWebClient;
        this.forwardingProperties = forwardingProperties;
        this.routingProvider = routingProvider;
    }

    public Mono<ResponseEntity<byte[]>> forward(
            ServerWebExchange exchange,
            String serviceMethod,
            byte[] requestBody) {

        String targetService = routingProvider.resolveService(serviceMethod);
        if (targetService == null || targetService.isBlank()) {
            return Mono.error(new RouteNotFoundException(serviceMethod));
        }

        String downstreamUrl = buildDownstreamUrl(targetService, exchange.getRequest().getURI().getRawPath());
        HttpMethod method = exchange.getRequest().getMethod();

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

    String buildDownstreamUrl(String serviceName, String originalPath) {
        String baseUrl = forwardingProperties.serviceUrlPattern()
                .replace("{serviceName}", serviceName);
        if (originalPath == null || originalPath.isEmpty()) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/") && originalPath.startsWith("/")) {
            return baseUrl + originalPath.substring(1);
        }
        if (!baseUrl.endsWith("/") && !originalPath.startsWith("/")) {
            return baseUrl + "/" + originalPath;
        }
        return baseUrl + originalPath;
    }

    private void copyRequestHeaders(HttpHeaders source, HttpHeaders target) {
        source.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                target.addAll(name, values);
            }
        });
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
        HttpHeaders target = new HttpHeaders();
        source.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                target.addAll(name, values);
            }
        });
        return target;
    }
}
