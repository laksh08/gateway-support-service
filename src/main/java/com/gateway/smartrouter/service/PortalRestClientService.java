package com.gateway.smartrouter.service;

import com.gateway.smartrouter.model.RestClientExecuteRequest;
import com.gateway.smartrouter.model.RestClientExecuteResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PortalRestClientService {

    private final WebClient webClient;

    public PortalRestClientService(WebClient forwardingWebClient) {
        this.webClient = forwardingWebClient;
    }

    public Mono<RestClientExecuteResponse> execute(RestClientExecuteRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.method().toUpperCase());
        Instant start = Instant.now();

        return webClient.method(method)
                .uri(request.url())
                .headers(headers -> {
                    if (request.headers() != null) {
                        request.headers().forEach(headers::set);
                    }
                })
                .body(request.body() != null && !request.body().isBlank()
                        ? BodyInserters.fromValue(request.body())
                        : BodyInserters.empty())
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            long durationMs = Duration.between(start, Instant.now()).toMillis();
                            Map<String, String> responseHeaders = new LinkedHashMap<>();
                            response.headers().asHttpHeaders().forEach((name, values) -> {
                                if (!values.isEmpty()) {
                                    responseHeaders.put(name, values.getFirst());
                                }
                            });
                            return new RestClientExecuteResponse(
                                    response.statusCode().value(),
                                    responseHeaders,
                                    body,
                                    durationMs
                            );
                        }))
                .timeout(Duration.ofSeconds(30));
    }
}
